package za.ac.alis.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.ac.alis.dto.ReportInfoDTO;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.ActionType;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.service.AiPipelineService;
import za.ac.alis.service.AuditLogService;
import za.ac.alis.service.DocumentService;
import za.ac.alis.service.SummaryReportService;

import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for compliance analysis.
 *
 * POST /api/compliance/analyze/{documentId}   — trigger (or re-trigger) the AI pipeline
 * GET  /api/compliance/status/{documentId}    — current document processing status
 * GET  /api/compliance/result/{documentId}    — the ONE SummaryReport for a document
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final DocumentService      documentService;
    private final AiPipelineService    aiPipelineService;
    private final SummaryReportService summaryReportService;
    private final AuditLogService      auditLogService;

    public ComplianceController(DocumentService documentService,
                                AiPipelineService aiPipelineService,
                                SummaryReportService summaryReportService,
                                AuditLogService auditLogService) {
        this.documentService      = documentService;
        this.aiPipelineService    = aiPipelineService;
        this.summaryReportService = summaryReportService;
        this.auditLogService      = auditLogService;
    }

    // ── POST /api/compliance/analyze/{documentId} ─────────────────────────────

    /**
     * Triggers or re-triggers the full Groq AI compliance pipeline.
     * The pipeline runs asynchronously — poll {@code /status} to track progress.
     *
     * Calling this on a document that was already ANALYZED will replace the
     * existing report with a fresh one.
     */
    @PostMapping("/analyze/{documentId}")
    public ResponseEntity<Map<String, Object>> triggerAnalysis(
            @PathVariable Long documentId,
            HttpServletRequest request) {

        Document doc = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        auditLogService.logDocumentAction(
                doc.getClient(), doc,
                ActionType.ANALYSIS_RUN,
                "Groq compliance analysis triggered for: " + doc.getTitle(),
                getClientIp(request));

        // Fire async pipeline (also handles re-analysis)
        aiPipelineService.processDocument(documentId);

        return ResponseEntity.accepted().body(Map.of(
                "message",    "Compliance analysis started",
                "documentId", documentId,
                "title",      doc.getTitle(),
                "currentStatus", doc.getStatus().name(),
                "note",       "Poll GET /api/compliance/status/" + documentId + " to check progress"
        ));
    }

    // ── GET /api/compliance/status/{documentId} ───────────────────────────────

    /**
     * Returns the document processing status and a summary of the report
     * (if analysis has completed).
     */
    @GetMapping("/status/{documentId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long documentId) {

        Document doc = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        Optional<SummaryReport> reportOpt =
                summaryReportService.findOneByDocumentId(documentId);

        Map<String, Object> response;

        if (reportOpt.isPresent()) {
            SummaryReport r = reportOpt.get();
            response = Map.of(
                    "documentId",       documentId,
                    "title",            doc.getTitle(),
                    "documentStatus",   doc.getStatus().name(),
                    "analysisStatus",   r.getAnalysisStatus() != null
                                            ? r.getAnalysisStatus().name() : "UNKNOWN",
                    "riskLevel",        r.getRiskLevel() != null
                                            ? r.getRiskLevel().name() : "UNKNOWN",
                    "similarityScore",  r.getSimilarityScore() != null
                                            ? r.getSimilarityScore() : 0,
                    "reportId",         r.getReportId(),
                    "reportReady",      doc.getStatus() == DocumentStat.ANALYZED
            );
        } else {
            response = Map.of(
                    "documentId",     documentId,
                    "title",          doc.getTitle(),
                    "documentStatus", doc.getStatus().name(),
                    "reportReady",    false,
                    "message",        doc.getStatus() == DocumentStat.PENDING
                                        || doc.getStatus() == DocumentStat.EXTRACTED
                                          ? "Analysis in progress — please wait"
                                          : "No report found. POST /api/compliance/analyze/"
                                              + documentId + " to trigger analysis"
            );
        }

        return ResponseEntity.ok(response);
    }

    // ── GET /api/compliance/result/{documentId} ───────────────────────────────

    /**
     * Returns the single SummaryReport for the document.
     * Returns 404 if analysis has not completed yet.
     */
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isBlank()) ? ip.split(",")[0] : req.getRemoteAddr();
    }
}
