package za.ac.alis.legal.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import za.ac.alis.core.dto.ReportInfoDTO;
import za.ac.alis.core.enums.ActionType;
import za.ac.alis.core.enums.DocumentStat;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.service.AiPipelineService;
import za.ac.alis.legal.service.AuditLogService;
import za.ac.alis.legal.service.DocumentService;
import za.ac.alis.legal.service.NotificationWebSocketService;
import za.ac.alis.legal.service.SummaryReportService;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final DocumentService documentService;
    private final AiPipelineService aiPipelineService;
    private final SummaryReportService summaryReportService;
    private final AuditLogService auditLogService;
    private final NotificationWebSocketService notificationWebSocketService;

    public ComplianceController(DocumentService documentService,
                                AiPipelineService aiPipelineService,
                                SummaryReportService summaryReportService,
                                AuditLogService auditLogService,
                                NotificationWebSocketService notificationWebSocketService) {
        this.documentService = documentService;
        this.aiPipelineService = aiPipelineService;
        this.summaryReportService = summaryReportService;
        this.auditLogService = auditLogService;
        this.notificationWebSocketService = notificationWebSocketService;
    }

    @PostMapping("/analyze/{documentId}")
    @PreAuthorize("hasAnyRole('LEGAL_PRACTITIONER','ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerAnalysis(
            @PathVariable Long documentId,
            HttpServletRequest request) {
        return startAnalysis(documentId, request);
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('LEGAL_PRACTITIONER','ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerAnalysisFromBody(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Object documentIdValue = body.get("documentId");
        if (documentIdValue == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "documentId is required"
            ));
        }
        Long documentId = documentIdValue instanceof Number number
                ? number.longValue()
                : Long.valueOf(documentIdValue.toString());
        return startAnalysis(documentId, request);
    }

    private ResponseEntity<Map<String, Object>> startAnalysis(
            Long documentId,
            HttpServletRequest request) {
        Document doc = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        auditLogService.logDocumentAction(
                doc.getClient(),
                doc,
                ActionType.ANALYSIS_RUN,
                "Groq compliance analysis triggered for: " + doc.getTitle(),
                getClientIp(request));

        notificationWebSocketService.notifyAnalysisStarted(
                doc,
                getAuthenticatedActorId(),
                getAuthenticatedActorRole());

        aiPipelineService.processDocument(documentId);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Compliance analysis started",
                "documentId", documentId,
                "title", doc.getTitle(),
                "currentStatus", doc.getStatus().name(),
                "note", "Poll GET /api/compliance/status/" + documentId + " to check progress"
        ));
    }

    @GetMapping("/status/{documentId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long documentId) {
        Document doc = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        Optional<SummaryReport> reportOpt =
                summaryReportService.findOneByDocumentId(documentId);

        if (reportOpt.isPresent()) {
            SummaryReport report = reportOpt.get();
            return ResponseEntity.ok(Map.of(
                    "documentId", documentId,
                    "title", doc.getTitle(),
                    "documentStatus", doc.getStatus().name(),
                    "analysisStatus", report.getAnalysisStatus() != null
                            ? report.getAnalysisStatus().name() : "UNKNOWN",
                    "riskLevel", report.getRiskLevel() != null
                            ? report.getRiskLevel().name() : "UNKNOWN",
                    "similarityScore", report.getSimilarityScore() != null
                            ? report.getSimilarityScore() : 0,
                    "reportId", report.getReportId(),
                    "reportReady", doc.getStatus() == DocumentStat.ANALYZED
            ));
        }

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "title", doc.getTitle(),
                "documentStatus", doc.getStatus().name(),
                "reportReady", false,
                "message", statusMessageWithoutReport(doc, documentId)
        ));
    }

    @GetMapping("/result/{documentId}")
    public ResponseEntity<ReportInfoDTO> getResult(@PathVariable Long documentId) {
        documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        SummaryReport report = summaryReportService.findOneByDocumentId(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No compliance report found for document " + documentId
                                + ". Trigger analysis first via POST /api/compliance/analyze/"
                                + documentId));

        return ResponseEntity.ok(ReportInfoDTO.fromEntity(report));
    }

    private String statusMessageWithoutReport(Document doc, Long documentId) {
        if (doc.getStatus() == DocumentStat.PENDING) {
            return "Document ingestion is in progress. Please wait for indexing to finish.";
        }
        if (doc.getStatus() == DocumentStat.EXTRACTED) {
            return "Document is indexed. POST /api/compliance/analyze/"
                    + documentId + " to trigger analysis.";
        }
        return "No report found. POST /api/compliance/analyze/"
                + documentId + " to trigger analysis.";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isBlank()) ? ip.split(",")[0] : request.getRemoteAddr();
    }

    private Long getAuthenticatedActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return Long.valueOf(principal);
        }
        return null;
    }

    private String getAuthenticatedActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .findFirst()
                .orElse(null);
    }
}
