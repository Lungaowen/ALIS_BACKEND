package za.ac.alis.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.ac.alis.dto.ReportInfoDTO;
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
    private final PdfReportService     pdfReportService;
    private final AuditLogService      auditLogService;

    public ReportController(SummaryReportService summaryReportService,
                            PdfReportService pdfReportService,
                            AuditLogService auditLogService) {
        this.summaryReportService = summaryReportService;
        this.pdfReportService     = pdfReportService;
        this.auditLogService      = auditLogService;
    }

    // GET /api/reports/{reportId}
   @GetMapping("/{reportId}")
public ResponseEntity<ReportInfoDTO> getReportById(@PathVariable Long reportId) {
    SummaryReport report = summaryReportService.findById(reportId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Report not found with id: " + reportId));
    return ResponseEntity.ok(toDTO(report));
}

private ReportInfoDTO toDTO(SummaryReport r) {
    return ReportInfoDTO.fromEntity(r);
}

    // GET /api/reports/document/{documentId}
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<ReportInfoDTO>> getReportsByDocument(@PathVariable Long documentId) {
        List<ReportInfoDTO> reports = summaryReportService.findByDocumentId(documentId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(reports);
    }

    // GET /api/reports/client/{clientId}
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ReportInfoDTO>> getReportsByClient(@PathVariable Long clientId) {
        List<ReportInfoDTO> reports = summaryReportService.findByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(reports);
    }

    // GET /api/reports/{reportId}/download-pdf
    @GetMapping("/{reportId}/download-pdf")
    public void downloadReportAsPdf(@PathVariable Long reportId,
                                    HttpServletResponse response,
                                    HttpServletRequest request) throws Exception {

        SummaryReport report = summaryReportService.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found with id: " + reportId));

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

    // ── Helpers ───────────────────────────────────────────────────────────────
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
    
}
