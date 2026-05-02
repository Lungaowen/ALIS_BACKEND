package za.ac.alis.controller;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import za.ac.alis.dto.DocumentResponseDTO;
import za.ac.alis.dto.ReportInfoDTO;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.SummaryReportRepository;
import za.ac.alis.service.DocumentService;
import za.ac.alis.service.PdfReportService;
import za.ac.alis.service.SummaryReportService;

@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasAnyRole('USER','LEGAL_PRACTITIONER','DEAL_MAKER')")
public class ClientDocumentController {

    private final DocumentService documentService;
    private final SummaryReportService summaryReportService;
    private final PdfReportService pdfReportService;
    private final SummaryReportRepository summaryReportRepository;
    private final DocumentRepository documentRepository;

    public ClientDocumentController(DocumentService documentService,
                                    SummaryReportService summaryReportService,
                                    PdfReportService pdfReportService,
                                    SummaryReportRepository summaryReportRepository,
                                    DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.summaryReportService = summaryReportService;
        this.pdfReportService = pdfReportService;
        this.summaryReportRepository = summaryReportRepository;
        this.documentRepository = documentRepository;
    }

    private Long getAuthenticatedClientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return Long.valueOf(principal);
        }
        throw new IllegalStateException("Not authenticated");
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) throws Exception {

        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.uploadDocument(file, clientId);

        return ResponseEntity.ok(Map.of(
                "message", "Document uploaded successfully",
                "documentId", doc.getDocumentId(),
                "title", doc.getTitle(),
                "status", doc.getStatus().name(),
                "fileUrl", doc.getFileUrl() != null ? doc.getFileUrl() : ""
        ));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponseDTO>> getMyDocuments() {
        Long clientId = getAuthenticatedClientId();
        List<DocumentResponseDTO> docs = documentService.getDocumentsByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable Long id) {
        Long clientId = getAuthenticatedClientId();
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(toDTO(doc));
    }

    @GetMapping("/documents/{documentId}/reports")
    public ResponseEntity<List<ReportInfoDTO>> getReportsForDocument(@PathVariable Long documentId) {
        Long clientId = getAuthenticatedClientId();

        if (!documentRepository.existsByDocumentIdAndClientId(documentId, clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }

        List<ReportInfoDTO> dtos = summaryReportService.findByDocumentIdWithRelations(documentId)
                .stream()
                .map(ReportInfoDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/reports/{reportId}/download")
    @Transactional
    public void downloadReport(@PathVariable Long reportId, HttpServletResponse response) throws IOException {
        Long clientId = getAuthenticatedClientId();
        SummaryReport report = summaryReportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!report.getDocument().getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
        }

        byte[] pdfBytes = pdfReportService.generateComplianceReportPdf(report);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=Compliance_Report_" + reportId + ".pdf");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    private DocumentResponseDTO toDTO(Document d) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setDocumentId(d.getDocumentId());
        dto.setTitle(d.getTitle());
        dto.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        dto.setIngestionSource(d.getIngestionSource() != null ? d.getIngestionSource().name() : null);
        dto.setUploadedAt(d.getUploadedAt() != null
                ? d.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        dto.setFilePath(d.getFilePath());
        dto.setFileUrl(d.getFileUrl());
        dto.setClientId(d.getClient() != null ? d.getClient().getClientId() : null);
        return dto;
    }
}
