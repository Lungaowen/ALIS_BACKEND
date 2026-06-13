package za.ac.alis.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.ac.alis.dto.DocumentResponseDTO;
import za.ac.alis.entities.Document;
import za.ac.alis.service.DocumentService;
import za.ac.alis.service.FirebaseStorageService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final FirebaseStorageService firebaseStorageService;

    public DocumentController(DocumentService documentService,
                              FirebaseStorageService firebaseStorageService) {
        this.documentService = documentService;
        this.firebaseStorageService = firebaseStorageService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with id: " + id));
        return ResponseEntity.ok(toDTO(document));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponseDTO>> getByClient(@PathVariable Long clientId) {
        List<DocumentResponseDTO> docs = documentService.getDocumentsByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponseDTO>> getAllDocuments() {
        List<DocumentResponseDTO> docs = documentService.getAllDocuments()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    @GetMapping("/{id}/download")
    public void downloadDocument(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Document doc = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file attached to this document");
        }

        byte[] fileBytes = firebaseStorageService.downloadFile(doc.getFilePath());
        String fileName = doc.getTitle() != null ? doc.getTitle() : "document_" + id;

        response.setContentType("application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"");
        response.setContentLength(fileBytes.length);
        response.getOutputStream().write(fileBytes);
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