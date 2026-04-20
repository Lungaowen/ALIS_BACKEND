package za.ac.alis.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import za.ac.alis.dto.DocumentResponseDTO;
import za.ac.alis.entities.Document;
import za.ac.alis.enums.ActionType;
import za.ac.alis.service.AuditLogService;
import za.ac.alis.service.DocumentService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AuditLogService auditLogService;

    public DocumentController(DocumentService documentService,
                              AuditLogService auditLogService) {
        this.documentService = documentService;
        this.auditLogService = auditLogService;
    }

    // POST /api/documents/upload
    @PostMapping("/upload")
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

    // GET /api/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with id: " + id));
        return ResponseEntity.ok(toDTO(document));
    }

    // GET /api/documents/client/{clientId}
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<DocumentResponseDTO>> getByClient(@PathVariable Long clientId) {
        List<DocumentResponseDTO> docs = documentService.getDocumentsByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    // GET /api/documents/all
    @GetMapping("/all")
    public ResponseEntity<List<DocumentResponseDTO>> getAllDocuments() {
        List<DocumentResponseDTO> docs = documentService.getAllDocuments()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
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
