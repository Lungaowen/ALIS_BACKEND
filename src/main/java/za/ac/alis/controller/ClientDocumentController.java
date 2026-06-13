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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import za.ac.alis.dto.DocumentResponseDTO;
import za.ac.alis.dto.DocumentUpdateRequest;
import za.ac.alis.dto.ReportInfoDTO;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.ActionType;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.SummaryReportRepository;
import za.ac.alis.service.AuditLogService;
import za.ac.alis.service.DocumentService;
import za.ac.alis.service.FirebaseStorageService;
import za.ac.alis.service.SummaryReportService;

@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasAnyRole('USER','LEGAL_PRACTITIONER','DEAL_MAKER')")
public class ClientDocumentController {

    private final DocumentService          documentService;
    private final SummaryReportService     summaryReportService;
    private final SummaryReportRepository  summaryReportRepository;
    private final DocumentRepository       documentRepository;
    private final FirebaseStorageService   firebaseStorageService;
    private final AuditLogService          auditLogService;

    public ClientDocumentController(DocumentService documentService,
                                    SummaryReportService summaryReportService,
                                    SummaryReportRepository summaryReportRepository,
                                    DocumentRepository documentRepository,
                                    FirebaseStorageService firebaseStorageService,
                                    AuditLogService auditLogService) {  // <-- ADD PARAMETER
        this.documentService = documentService;
        this.summaryReportService = summaryReportService;
        this.summaryReportRepository = summaryReportRepository;
        this.documentRepository = documentRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.auditLogService = auditLogService;  // <-- INITIALIZE
    }

    // ── Auth helper ───────────────────────────────────────────────────────────
    private Long getAuthenticatedClientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return Long.valueOf(principal);
        }
        throw new IllegalStateException("Not authenticated");
    }

    // ── Create ────────────────────────────────────────────────────────────────
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasAnyRole('USER','LEGAL_PRACTITIONER','DEAL_MAKER')")
public ResponseEntity<Map<String, Object>> uploadDocumentSecure(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "documentType", required = false) String documentType,
        @RequestParam(value = "jurisdiction", required = false) String jurisdiction,
        HttpServletRequest request) throws Exception {

    Long clientId = getAuthenticatedClientId();
    Document doc = documentService.uploadDocument(file, clientId, documentType, jurisdiction);
    auditLogService.logDocumentAction(doc.getClient(), doc, ActionType.UPLOAD_DOCUMENT,
            "Document queued for AI processing", getClientIp(request));
    return ResponseEntity.accepted().body(Map.of(
            "message", "Document uploaded and queued for processing",
            "documentId", doc.getDocumentId(),
            "status", doc.getStatus().name()
    ));
}

    // Add the getClientIp helper
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
    // ── Read (list) ───────────────────────────────────────────────────────────
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponseDTO>> getMyDocuments() {
        Long clientId = getAuthenticatedClientId();
        List<DocumentResponseDTO> docs = documentService.getDocumentsByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    // ── Read (single) ─────────────────────────────────────────────────────────
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

    // ── Read (reports for a document) ─────────────────────────────────────────
    
    // ── Update (metadata only — title) ────────────────────────────────────────
    @PatchMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDTO> updateMyDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateRequest request) {

        Long clientId = getAuthenticatedClientId();

        try {
            Document updated = documentService.updateDocument(id, clientId, request.getTitle());
            return ResponseEntity.ok(toDTO(updated));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found"));
        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }

        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    // ── View (inline browser preview) ─────────────────────────────────────────
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No file attached to this document");
        }

        byte[] fileBytes = firebaseStorageService.downloadFile(doc.getFilePath());
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + doc.getTitle() + "\"");
        response.setContentLength(fileBytes.length);
        response.getOutputStream().write(fileBytes);
        response.getOutputStream().flush();
    }

    // ── Download (force save-dialog) ──────────────────────────────────────────
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No file attached to this document");
        }

        byte[] fileBytes = firebaseStorageService.downloadFile(doc.getFilePath());
        String fileName  = doc.getTitle() != null ? doc.getTitle() : "document_" + id;
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"");
        response.setContentLength(fileBytes.length);
        response.getOutputStream().write(fileBytes);
        response.getOutputStream().flush();
    }

    // ── Report download ───────────────────────────────────────────────────────
  

    // ── DTO mapper ────────────────────────────────────────────────────────────
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
    // ── Reports ───────────────────────────────────────────────────────────────
@GetMapping("/reports")
public ResponseEntity<List<ReportInfoDTO>> getMyReports() {
    Long clientId = getAuthenticatedClientId();
    List<SummaryReport> reports = summaryReportRepository.findReportsByClientIdEager(clientId);
    List<ReportInfoDTO> dtos = reports.stream()
            .map(ReportInfoDTO::fromEntity)
            .toList();
    return ResponseEntity.ok(dtos);
}

@GetMapping("/documents/{documentId}/report")
public ResponseEntity<ReportInfoDTO> getReportForDocument(@PathVariable Long documentId) {
    Long clientId = getAuthenticatedClientId();
    // Verify document belongs to client
    Document doc = documentService.getDocumentById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!doc.getClient().getClientId().equals(clientId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    SummaryReport report = summaryReportService.findOneByDocumentId(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No report found for this document"));
    return ResponseEntity.ok(ReportInfoDTO.fromEntity(report));
}

@GetMapping("/reports/{reportId}")
public ResponseEntity<ReportInfoDTO> getReportById(@PathVariable Long reportId) {
    Long clientId = getAuthenticatedClientId();
    SummaryReport report = summaryReportService.findById(reportId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    // Ensure report belongs to the authenticated client
    if (!report.getClient().getClientId().equals(clientId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return ResponseEntity.ok(ReportInfoDTO.fromEntity(report));
}
}
