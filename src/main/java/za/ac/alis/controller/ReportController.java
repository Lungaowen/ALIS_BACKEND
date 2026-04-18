package za.ac.alis.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.ActionType;
import za.ac.alis.service.AuditLogService;
import za.ac.alis.service.PdfReportService;
import za.ac.alis.service.SummaryReportService;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SummaryReportService summaryReportService;
    private final PdfReportService pdfReportService;
    private final AuditLogService auditLogService;

    public ReportController(SummaryReportService summaryReportService,
                            PdfReportService pdfReportService,
                            AuditLogService auditLogService) {
        this.summaryReportService = summaryReportService;
        this.pdfReportService = pdfReportService;
        this.auditLogService = auditLogService;
    }

    // ── GET REPORT BY ID ──────────────────────────────────────────────────────
    @GetMapping("/{reportId}")
    public ResponseEntity<SummaryReport> getReportById(@PathVariable Long reportId) {
        SummaryReport report = summaryReportService.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found with id: " + reportId));
        return ResponseEntity.ok(report);
    }

    // ── GET ALL REPORTS FOR A DOCUMENT ────────────────────────────────────────
    @GetMapping("/document/{documentId}")
    public ResponseEntity<Object> getReportsByDocument(
            @PathVariable Long documentId) {
        return ResponseEntity.ok(summaryReportService.findByDocumentId(documentId));
    }

    // ── GET ALL REPORTS FOR A CLIENT ──────────────────────────────────────────
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Object> getReportsByClient(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(summaryReportService.findByClientId(clientId));
    }

    // ── DOWNLOAD PDF ──────────────────────────────────────────────────────────
    @GetMapping("/{reportId}/download-pdf")
    public void downloadReportAsPdf(@PathVariable Long reportId,
                                    HttpServletResponse response,
                                    HttpServletRequest request) throws Exception {

        // ✅ Returns 404 instead of 500 if not found
        SummaryReport report = summaryReportService.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found with id: " + reportId));

        byte[] pdfBytes = pdfReportService.generateComplianceReportPdf(report);

        // ✅ Safe null check before audit logging
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

    // ── IP HELPER ─────────────────────────────────────────────────────────────
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
}
