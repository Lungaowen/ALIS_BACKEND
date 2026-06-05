package za.ac.alis.legal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import za.ac.alis.core.dto.DocumentResponseDTO;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.enums.ActionType;
import za.ac.alis.legal.service.AuditLogService;
import za.ac.alis.legal.service.DocumentService;
import za.ac.alis.legal.service.FirebaseStorageService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AuditLogService auditLogService;
    private final FirebaseStorageService firebaseStorageService;

    public DocumentController(DocumentService documentService,
                              AuditLogService auditLogService,
                              FirebaseStorageService firebaseStorageService) {
        this.documentService = documentService;
        this.auditLogService = auditLogService;
        this.firebaseStorageService = firebaseStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file")     MultipartFile file,
            @RequestParam("clientId") Long clientId,
            HttpServletRequest request) throws Exception {

        Document document = documentService.uploadDocument(file, clientId);

        auditLogService.logDocumentAction(
                document.getClient(),
                document,
                ActionType.UPLOAD_DOCUMENT,
                "Document uploaded: " + document.getTitle(),
                getClientIp(request)
        );

        return ResponseEntity.ok(Map.of(
                "message",    "Document uploaded successfully",
                "documentId", document.getDocumentId(),
                "title",      document.getTitle(),
                "status",     document.getStatus().name(),
                "fileUrl",    document.getFileUrl() != null ? document.getFileUrl() : ""
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with id: " + id));
        return ResponseEntity.ok(toDTO(document));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<DocumentResponseDTO>> getByClient(@PathVariable Long clientId) {
        List<DocumentResponseDTO> docs = documentService.getDocumentsByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DocumentResponseDTO>> getAllDocuments() {
        List<DocumentResponseDTO> docs = documentService.getAllDocuments()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    // ── NEW: download endpoint ───────────────────────────────────────────────
    @GetMapping("/{id}/download")
    public void downloadDocument(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file attached to this document");
        }

        byte[] fileBytes = firebaseStorageService.downloadFile(doc.getFilePath());
        String fileName = doc.getTitle() != null ? doc.getTitle() : "document_" + id;
        String mimeType = "application/octet-stream"; // generic; can be refined with doc metadata if needed

        response.setContentType(mimeType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"");
        response.setContentLength(fileBytes.length);
        response.getOutputStream().write(fileBytes);
        response.getOutputStream().flush();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
}