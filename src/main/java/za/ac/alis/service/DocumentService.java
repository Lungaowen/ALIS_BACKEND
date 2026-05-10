package za.ac.alis.service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

// ★ replaced import
import za.ac.alis.service.aiengine.AiEngineClient;

@Service
public class DocumentService {

    private static final Logger log            = LoggerFactory.getLogger(DocumentService.class);
    private static final long   MAX_FILE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final DocumentRepository        documentRepository;
    private final ClientRepository          clientRepository;
    private final FileMetadataRepository    fileMetadataRepository;
    private final DocumentContentRepository documentContentRepository;
    private final FirebaseStorageService    firebaseStorageService;

    // ★ AiEngineClient replaces AiPipelineService
    private final AiEngineClient            aiEngineClient;

    private final SummaryReportRepository   summaryReportRepository;
    private final AuditLogRepository        auditLogRepository;

    public DocumentService(DocumentRepository documentRepository,
                           ClientRepository clientRepository,
                           FileMetadataRepository fileMetadataRepository,
                           DocumentContentRepository documentContentRepository,
                           FirebaseStorageService firebaseStorageService,
                           AiEngineClient aiEngineClient,            // ★ new dependency
                           SummaryReportRepository summaryReportRepository,
                           AuditLogRepository auditLogRepository) {
        this.documentRepository        = documentRepository;
        this.clientRepository          = clientRepository;
        this.fileMetadataRepository    = fileMetadataRepository;
        this.documentContentRepository = documentContentRepository;
        this.firebaseStorageService    = firebaseStorageService;
        this.aiEngineClient            = aiEngineClient;
        this.summaryReportRepository   = summaryReportRepository;
        this.auditLogRepository        = auditLogRepository;
    }

    // ── Create (Upload) ───────────────────────────────────────────────────────
    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }

        String mimeType     = file.getContentType();
        String safeFilename = FilenameSanitizer.sanitize(file.getOriginalFilename(), mimeType);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        String hash;
        try (InputStream is = file.getInputStream()) {
            hash = computeSha256(is);
        }
        if (fileMetadataRepository.existsByHash(hash)) {
            throw new RuntimeException(
                    "Duplicate file detected. This document has already been uploaded.");
        }

        Document doc = new Document();
        doc.setTitle(safeFilename);
        doc.setClient(client);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus(DocumentStat.PENDING);
        doc.setIngestionSource(IngestionSource.MANUAL);
        Document savedDoc = documentRepository.saveAndFlush(doc);
        log.info("Created Document ID={} for client={}", savedDoc.getDocumentId(), clientId);

        FirebaseStorageService.StorageResult upload;
        try (InputStream is = file.getInputStream()) {
            upload = firebaseStorageService.uploadFile(is, safeFilename, mimeType, clientId);
        } catch (Exception e) {
            log.error("Firebase upload failed for Document ID={}", savedDoc.getDocumentId(), e);
            savedDoc.setStatus(DocumentStat.FAILED);
            documentRepository.saveAndFlush(doc);
            throw new RuntimeException("Failed to upload file to cloud storage", e);
        }

        savedDoc.setFilePath(upload.getObjectPath());
        savedDoc.setFileUrl(upload.getSignedUrl());
        documentRepository.saveAndFlush(doc);

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

        // ★ Trigger the AI engine's analysis pipeline
        log.info("Triggering AI engine analysis for Document ID={}", savedDoc.getDocumentId());
        aiEngineClient.triggerAnalysis(savedDoc.getDocumentId());

        return savedDoc;
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
}