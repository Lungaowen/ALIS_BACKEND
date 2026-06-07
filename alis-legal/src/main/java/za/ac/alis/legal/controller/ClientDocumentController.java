package za.ac.alis.legal.controller;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import za.ac.alis.core.dto.DocumentResponseDTO;
import za.ac.alis.core.dto.DocumentUpdateRequest;
import za.ac.alis.core.dto.ReportDownloadUrlDTO;
import za.ac.alis.core.dto.ReportInfoDTO;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.persistence.DocumentRepository;
import za.ac.alis.legal.persistence.SummaryReportRepository;
import za.ac.alis.legal.service.DocumentService;
import za.ac.alis.legal.service.FirebaseStorageService;
import za.ac.alis.legal.service.PdfReportService;
import za.ac.alis.legal.service.ReportArtifactService;
import za.ac.alis.legal.service.SummaryReportService;

@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasAnyRole('USER','LEGAL_PRACTITIONER','DEAL_MAKER')")
public class ClientDocumentController {

    private final DocumentService          documentService;
    private final SummaryReportService     summaryReportService;
    private final PdfReportService         pdfReportService;
    private final SummaryReportRepository  summaryReportRepository;
    private final DocumentRepository       documentRepository;
    private final FirebaseStorageService   firebaseStorageService;
    private final ReportArtifactService    reportArtifactService;

    public ClientDocumentController(DocumentService documentService,
                                    SummaryReportService summaryReportService,
                                    PdfReportService pdfReportService,
                                    SummaryReportRepository summaryReportRepository,
                                    DocumentRepository documentRepository,
                                    FirebaseStorageService firebaseStorageService,
                                    ReportArtifactService reportArtifactService) {
        this.documentService         = documentService;
        this.summaryReportService    = summaryReportService;
        this.pdfReportService        = pdfReportService;
        this.summaryReportRepository = summaryReportRepository;
        this.documentRepository      = documentRepository;
        this.firebaseStorageService  = firebaseStorageService;
        this.reportArtifactService   = reportArtifactService;
    }

    // ── Auth helper ───────────────────────────────────────────────────────────
    private Long getAuthenticatedClientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return Long.valueOf(principal);
        }
        throw new IllegalStateException("Not authenticated");
    }

    // ── Upload ────────────────────────────────────────────────────────────────
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) throws Exception {

        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.uploadDocument(file, clientId);

        return ResponseEntity.ok(Map.of(
                "message",    "Document uploaded successfully",
                "documentId", doc.getDocumentId(),
                "title",      doc.getTitle(),
                "status",     doc.getStatus().name(),
                "fileUrl",    doc.getFileUrl() != null ? doc.getFileUrl() : ""
        ));
    }

    // ── List own documents (with latest report data) ──────────────────────────
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponseDTO>> getMyDocuments() {
        Long clientId = getAuthenticatedClientId();
        List<DocumentResponseDTO> docs = documentService.getDocumentsByClientId(clientId)
                .stream()
                .map(d -> toDTOWithReport(d, clientId))
                .toList();
        return ResponseEntity.ok(docs);
    }

    // ── Single document ───────────────────────────────────────────────────────
    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable Long id) {
        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(toDTOWithReport(doc, clientId));
    }

    // ── Reports for a document ────────────────────────────────────────────────
    @GetMapping("/documents/{documentId}/reports")
    public ResponseEntity<List<ReportInfoDTO>> getReportsForDocument(
            @PathVariable Long documentId) {

        Long clientId = getAuthenticatedClientId();
        if (!documentRepository.existsByDocumentIdAndClientId(documentId, clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }

        List<ReportInfoDTO> dtos = summaryReportService
                .findByDocumentIdWithRelations(documentId)
                .stream()
                .map(ReportInfoDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ── Update title ──────────────────────────────────────────────────────────
    @PatchMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDTO> updateMyDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateRequest request) {

        Long clientId = getAuthenticatedClientId();
        try {
            Document updated = documentService.updateDocument(id, clientId, request.getTitle());
            return ResponseEntity.ok(toDTOWithReport(updated, clientId));
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, String>> deleteMyDocument(@PathVariable Long id) {
        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    // ── View (inline) ─────────────────────────────────────────────────────────
    @GetMapping("/documents/{id}/view")
    public void viewDocument(@PathVariable Long id, HttpServletResponse response)
            throws IOException {

        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file attached");
        }
        byte[] bytes = firebaseStorageService.downloadFile(doc.getFilePath());
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getTitle() + "\"");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    // ── Download (force save) ─────────────────────────────────────────────────
    @GetMapping("/documents/{id}/download")
    public void downloadDocument(@PathVariable Long id, HttpServletResponse response)
            throws IOException {

        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file attached");
        }
        byte[] bytes = firebaseStorageService.downloadFile(doc.getFilePath());
        String name  = doc.getTitle() != null ? doc.getTitle() : "document_" + id;
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    // ── Report PDF download ───────────────────────────────────────────────────
    @GetMapping("/reports/{reportId}/download")
    @Transactional
    public void downloadReport(@PathVariable Long reportId, HttpServletResponse response)
            throws IOException {

        Long clientId = getAuthenticatedClientId();
        SummaryReport report = summaryReportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!report.getDocument().getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
        }

        // Serve Firebase PDF if available, otherwise generate
        if (report.getReportUrl() != null && !report.getReportUrl().isBlank()) {
            response.sendRedirect(report.getReportUrl());
            return;
        }

        byte[] pdfBytes = pdfReportService.generateComplianceReportPdf(report);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=Compliance_Report_" + reportId + ".pdf");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    // ── Report download URL ───────────────────────────────────────────────────
    @GetMapping("/reports/{reportId}/download-url")
    @Transactional
    public ResponseEntity<ReportDownloadUrlDTO> getReportDownloadUrl(@PathVariable Long reportId)
            throws IOException {

        Long clientId = getAuthenticatedClientId();
        SummaryReport report = summaryReportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!report.getDocument().getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
        }

        try {
            return ResponseEntity.ok(reportArtifactService.getSignedDownloadUrl(report));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, e.getMessage(), e);
        }
    }

    // ── DTO mappers ───────────────────────────────────────────────────────────

    /**
     * Maps a Document to DTO enriched with latest summary_report data.
     * Uses a separate query per document — acceptable for typical list sizes.
     */
    private DocumentResponseDTO toDTOWithReport(Document d, Long clientId) {
        DocumentResponseDTO dto = toDTO(d);

        // Fetch latest report for this document
        List<SummaryReport> reports = summaryReportRepository
                .findByDocumentDocumentId(d.getDocumentId());

        if (reports != null && !reports.isEmpty()) {
            SummaryReport latest = reports.stream()
                    .max(Comparator.comparing(SummaryReport::getGeneratedAt))
                    .orElse(null);
            if (latest != null) {
                dto.setReportId(latest.getReportId());
                dto.setRiskLevel(latest.getRiskLevel() != null
                        ? latest.getRiskLevel().name() : null);
                dto.setSimilarityScore(latest.getSimilarityScore() != null
                        ? latest.getSimilarityScore().doubleValue() : null);
            }
        }
        return dto;
    }

    private DocumentResponseDTO toDTO(Document d) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setDocumentId(d.getDocumentId());
        dto.setTitle(d.getTitle());
        dto.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        dto.setIngestionSource(d.getIngestionSource() != null
                ? d.getIngestionSource().name() : null);
        dto.setUploadedAt(d.getUploadedAt() != null
                ? d.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        dto.setFilePath(d.getFilePath());
        dto.setFileUrl(d.getFileUrl());
        dto.setClientId(d.getClient() != null ? d.getClient().getClientId() : null);
        return dto;
    }
}
