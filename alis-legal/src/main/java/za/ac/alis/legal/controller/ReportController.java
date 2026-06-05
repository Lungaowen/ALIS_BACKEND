package za.ac.alis.legal.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import za.ac.alis.core.dto.ReportContentUpdateRequest;
import za.ac.alis.core.dto.ReportDownloadUrlDTO;
import za.ac.alis.core.dto.ReportInfoDTO;
import za.ac.alis.core.enums.ActionType;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.service.AuditLogService;
import za.ac.alis.legal.service.PdfReportService;
import za.ac.alis.legal.service.ReportArtifactService;
import za.ac.alis.legal.service.SummaryReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SummaryReportService summaryReportService;
    private final PdfReportService pdfReportService;
    private final ReportArtifactService reportArtifactService;
    private final AuditLogService auditLogService;

    public ReportController(SummaryReportService summaryReportService,
                            PdfReportService pdfReportService,
                            ReportArtifactService reportArtifactService,
                            AuditLogService auditLogService) {
        this.summaryReportService = summaryReportService;
        this.pdfReportService = pdfReportService;
        this.reportArtifactService = reportArtifactService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportInfoDTO> getReportById(@PathVariable Long reportId) {
        SummaryReport report = getReport(reportId);
        return ResponseEntity.ok(toDTO(report));
    }

    @PatchMapping("/{reportId}")
    public ResponseEntity<ReportInfoDTO> updateReportContent(
            @PathVariable Long reportId,
            @RequestBody ReportContentUpdateRequest request) {

        SummaryReport report = getReport(reportId);
        assertCanAccess(report);
        SummaryReport updated = summaryReportService.updateAiContent(
                reportId,
                request != null ? request.getAiExplanation() : null,
                request != null ? request.getAiRecommendation() : null);

        return ResponseEntity.ok(toDTO(updated));
    }

    @GetMapping("/{reportId}/download-url")
    public ResponseEntity<ReportDownloadUrlDTO> getReportDownloadUrl(@PathVariable Long reportId)
            throws Exception {

        SummaryReport report = getReport(reportId);
        assertCanAccess(report);
        try {
            return ResponseEntity.ok(reportArtifactService.getSignedDownloadUrl(report));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, e.getMessage(), e);
        }
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<ReportInfoDTO>> getReportsByDocument(@PathVariable Long documentId) {
        List<ReportInfoDTO> reports = summaryReportService.findByDocumentId(documentId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ReportInfoDTO>> getReportsByClient(@PathVariable Long clientId) {
        List<ReportInfoDTO> reports = summaryReportService.findByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{reportId}/download-pdf")
    public void downloadReportAsPdf(@PathVariable Long reportId,
                                    HttpServletResponse response,
                                    HttpServletRequest request) throws Exception {

        SummaryReport report = getReport(reportId);
        assertCanAccess(report);
        byte[] pdfBytes = pdfReportService.generateComplianceReportPdf(report);

        if (report.getClient() != null && report.getDocument() != null) {
            auditLogService.logDocumentAction(
                    report.getClient(),
                    report.getDocument(),
                    ActionType.ANALYSIS_RUN,
                    "PDF downloaded for report ID: " + reportId,
                    getClientIp(request)
            );
        }

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=Compliance_Report_" + reportId + ".pdf");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    private SummaryReport getReport(Long reportId) {
        return summaryReportService.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found with id: " + reportId));
    }

    private ReportInfoDTO toDTO(SummaryReport report) {
        return ReportInfoDTO.fromEntity(report);
    }

    private void assertCanAccess(SummaryReport report) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (hasRole(authentication, "ADMIN")) {
            return;
        }
        Long authenticatedClientId = authenticatedClientId(authentication);
        Long reportClientId = report.getClient() != null ? report.getClient().getClientId() : null;
        if (authenticatedClientId != null && authenticatedClientId.equals(reportClientId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
    }

    private boolean hasRole(Authentication authentication, String role) {
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private Long authenticatedClientId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
}
