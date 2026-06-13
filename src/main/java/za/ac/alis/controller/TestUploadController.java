package za.ac.alis.controller;

import za.ac.alis.entities.Document;
import za.ac.alis.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestUploadController {

    private final DocumentService documentService;

    public TestUploadController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadTest(
            @RequestParam("file") MultipartFile file,
            @RequestParam("clientId") Long clientId) {

        try {
            Document savedDoc = documentService.uploadDocument(file, clientId, "TEST", "South Africa");
            return ResponseEntity.ok("✅ Upload successful! Document ID: " + savedDoc.getDocumentId() 
                + "\nStatus: " + savedDoc.getStatus());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Upload failed: " + e.getMessage());
        }
    }
}