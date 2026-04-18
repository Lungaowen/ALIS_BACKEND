package za.ac.alis.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import za.ac.alis.entities.Document;
import za.ac.alis.enums.ActionType;
import za.ac.alis.service.AuditLogService;
import za.ac.alis.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final AuditLogService auditLogService;

    public DocumentController(DocumentService documentService,
                              AuditLogService auditLogService) {
        this.documentService = documentService;
        this.auditLogService = auditLogService;
    }

    // ── UPLOAD ────────────────────────────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
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
                "message", "Document uploaded successfully",
                "documentId", document.getDocumentId(),
                "title", document.getTitle(),
                "status", document.getStatus()
        ));
    }

    // ── GET ONE DOCUMENT ──────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with id: " + id));
        return ResponseEntity.ok(document);
    }

    // ── GET ALL DOCUMENTS FOR A CLIENT ────────────────────────────────────────
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Document>> getDocumentsByClient(@PathVariable Long clientId) {
        List<Document> documents = documentService.getDocumentsByClientId(clientId);
        return ResponseEntity.ok(documents);
    }

    // ── GET ALL DOCUMENTS (admin use) ─────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    // ── DELETE DOCUMENT ───────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    // ── IP HELPER ─────────────────────────────────────────────────────────────
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
}
