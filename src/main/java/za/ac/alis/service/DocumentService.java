package za.ac.alis.service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

import za.ac.alis.entities.Client;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import za.ac.alis.entities.FileMetadata;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.IngestionSource;
import za.ac.alis.repo.AuditLogRepository;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.repo.DocumentContentRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.FileMetadataRepository;
import za.ac.alis.repo.SummaryReportRepository;
import za.ac.alis.utils.FilenameSanitizer;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final DocumentContentRepository documentContentRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final SummaryReportRepository summaryReportRepository;
    private final AuditLogRepository auditLogRepository;

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    public DocumentService(DocumentRepository documentRepository,
                           ClientRepository clientRepository,
                           FileMetadataRepository fileMetadataRepository,
                           DocumentContentRepository documentContentRepository,
                           FirebaseStorageService firebaseStorageService,
                           SummaryReportRepository summaryReportRepository,
                           AuditLogRepository auditLogRepository,
                           @Value("${alis.python.service.url}") String pythonBaseUrl) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.documentContentRepository = documentContentRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.summaryReportRepository = summaryReportRepository;
        this.auditLogRepository = auditLogRepository;
        this.pythonBaseUrl = pythonBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    // ── Create (Upload) ── Forward to Python ─────────────────────────────────
    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId, String documentType, String jurisdiction) throws Exception {

        // 1. Validation & duplicate check
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }

        String mimeType = file.getContentType();
        String safeFilename = FilenameSanitizer.sanitize(file.getOriginalFilename(), mimeType);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        String hash;
        try (InputStream is = file.getInputStream()) {
            hash = computeSha256(is);
        }
        if (fileMetadataRepository.existsByHash(hash)) {
            throw new RuntimeException("Duplicate file detected. This document has already been uploaded.");
        }

        // 2. Create document record (pending)
        Document doc = new Document();
        doc.setTitle(safeFilename);
        doc.setClient(client);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus(DocumentStat.PENDING);
        doc.setIngestionSource(IngestionSource.MANUAL);
        Document savedDoc = documentRepository.saveAndFlush(doc);
        log.info("Created Document ID={} for client={}", savedDoc.getDocumentId(), clientId);

        // 3. Forward to Python service
        PythonProcessResponse pyResponse;
        try {
            pyResponse = callPythonProcess(file, savedDoc, documentType, jurisdiction);
        } catch (Exception e) {
            log.error("Python service call failed for Document ID={}", savedDoc.getDocumentId(), e);
            savedDoc.setStatus(DocumentStat.FAILED);
            documentRepository.save(savedDoc);
            throw new RuntimeException("Failed to queue document for AI processing", e);
        }

        // 4. Update document status to PROCESSING
        savedDoc.setStatus(DocumentStat.PROCESSING);
        documentRepository.save(savedDoc);

        // 5. Save minimal metadata (no file path/url yet – Python will update later)
        FileMetadata meta = new FileMetadata();
        meta.setDocument(savedDoc);
        meta.setMimeType(mimeType);
        meta.setSize(file.getSize());
        meta.setHash(hash);
        meta.setUploadedAt(LocalDateTime.now());
        fileMetadataRepository.save(meta);

        DocumentContent content = new DocumentContent();
        content.setDocument(savedDoc);
        documentContentRepository.save(content);

        return savedDoc;
    }

    private PythonProcessResponse callPythonProcess(MultipartFile file, Document document, String documentType, String jurisdiction) throws Exception {
        String url = pythonBaseUrl + "/api/process";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });
        body.add("document_id", document.getDocumentId());
        body.add("client_id", document.getClient().getClientId());
        body.add("document_title", document.getTitle());
        body.add("document_type", documentType != null ? documentType : "UNKNOWN");
        body.add("jurisdiction", jurisdiction != null ? jurisdiction : "South Africa");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<PythonProcessResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, PythonProcessResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Python service returned error: " + response.getStatusCode());
        }
        return response.getBody();
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public List<Document> getDocumentsByClientId(Long clientId) {
        return documentRepository.findByClient_ClientId(clientId);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // ── Update ────────────────────────────────────────────────────────────────
    @Transactional
    public Document updateDocument(Long id, Long clientId, String newTitle) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        if (!doc.getClient().getClientId().equals(clientId)) {
            throw new SecurityException("Not your document");
        }

        if (newTitle != null && !newTitle.isBlank()) {
            doc.setTitle(newTitle.trim());
        }

        Document updated = documentRepository.save(doc);
        log.info("Updated Document ID={} — new title='{}'", id, updated.getTitle());
        return updated;
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @Transactional
    public void deleteDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        summaryReportRepository.deleteByDocument_DocumentId(id);
        auditLogRepository.deleteByDocumentId(id);
        documentContentRepository.findByDocument(doc).ifPresent(documentContentRepository::delete);
        fileMetadataRepository.findByDocument(doc).ifPresent(fileMetadataRepository::delete);

        if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
            firebaseStorageService.deleteFileByPath(doc.getFilePath());
        }
        documentRepository.delete(doc);
        log.info("Deleted Document ID={}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String computeSha256(InputStream is) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            digest.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    // Inner DTO for Python response
    private static class PythonProcessResponse {
        @JsonProperty("task_id")
        private String taskId;
        private String status;
        @JsonProperty("document_id")
        private Long documentId;
        @JsonProperty("file_name")
        private String fileName;

        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public Long getDocumentId() { return documentId; }
        public String getFileName() { return fileName; }
    }
}