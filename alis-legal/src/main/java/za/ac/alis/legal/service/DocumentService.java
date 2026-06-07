package za.ac.alis.legal.service;

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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.DocumentContent;
import za.ac.alis.core.persistence.FileMetadata;
import za.ac.alis.core.enums.DocumentStat;
import za.ac.alis.core.enums.IngestionSource;
import za.ac.alis.legal.persistence.AuditLogRepository;
import za.ac.alis.user.persistence.ClientRepository;
import za.ac.alis.legal.persistence.DocumentContentRepository;
import za.ac.alis.legal.persistence.DocumentRepository;
import za.ac.alis.legal.persistence.FileMetadataRepository;
import za.ac.alis.legal.persistence.SummaryReportRepository;
import za.ac.alis.core.util.FilenameSanitizer;

@Service
public class DocumentService {

    private static final Logger log            = LoggerFactory.getLogger(DocumentService.class);
    private static final long   MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB — match Python gateway

    private final DocumentRepository            documentRepository;
    private final ClientRepository              clientRepository;
    private final FileMetadataRepository        fileMetadataRepository;
    private final DocumentContentRepository     documentContentRepository;
    private final FirebaseStorageService        firebaseStorageService;
    private final NotificationWebSocketService  notificationWebSocketService;
    private final DocumentTextIndexingService   documentTextIndexingService;
    private final SummaryReportRepository       summaryReportRepository;
    private final AuditLogRepository            auditLogRepository;

    public DocumentService(DocumentRepository documentRepository,
                           ClientRepository clientRepository,
                           FileMetadataRepository fileMetadataRepository,
                           DocumentContentRepository documentContentRepository,
                           FirebaseStorageService firebaseStorageService,
                           NotificationWebSocketService notificationWebSocketService,
                           DocumentTextIndexingService documentTextIndexingService,
                           SummaryReportRepository summaryReportRepository,
                           AuditLogRepository auditLogRepository) {
        this.documentRepository           = documentRepository;
        this.clientRepository             = clientRepository;
        this.fileMetadataRepository       = fileMetadataRepository;
        this.documentContentRepository    = documentContentRepository;
        this.firebaseStorageService       = firebaseStorageService;
        this.notificationWebSocketService = notificationWebSocketService;
        this.documentTextIndexingService  = documentTextIndexingService;
        this.summaryReportRepository      = summaryReportRepository;
        this.auditLogRepository           = auditLogRepository;
    }

    // ── Upload (Java-side) ────────────────────────────────────────────────────
    // Used by ClientDocumentController and DocumentController.
    // Uploads the file to Firebase and creates the document row.
    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum size of 50 MB");
        }

        String mimeType     = file.getContentType();
        String safeFilename = FilenameSanitizer.sanitize(file.getOriginalFilename(), mimeType);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        // Duplicate detection
        String hash;
        try (InputStream is = file.getInputStream()) {
            hash = computeSha256(is);
        }
        if (fileMetadataRepository.existsByHash(hash)) {
            throw new RuntimeException(
                    "Duplicate file detected. This document has already been uploaded.");
        }

        // Create document row immediately so listing works before Firebase completes
        Document doc = new Document();
        doc.setTitle(safeFilename);
        doc.setClient(client);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus(DocumentStat.PENDING);
        doc.setIngestionSource(IngestionSource.UPLOAD); // was MANUAL — corrected
        Document savedDoc = documentRepository.saveAndFlush(doc);
        log.info("Created Document ID={} for client={}", savedDoc.getDocumentId(), clientId);

        // Upload to Firebase
        FirebaseStorageService.StorageResult upload;
        try (InputStream is = file.getInputStream()) {
            upload = firebaseStorageService.uploadFile(is, safeFilename, mimeType, clientId);
        } catch (Exception e) {
            log.error("Firebase upload failed for Document ID={}", savedDoc.getDocumentId(), e);
            savedDoc.setStatus(DocumentStat.FAILED);
            documentRepository.saveAndFlush(savedDoc);
            throw new RuntimeException("Failed to upload file to cloud storage", e);
        }

        savedDoc.setFilePath(upload.getObjectPath());
        savedDoc.setFileUrl(upload.getSignedUrl());
        documentRepository.saveAndFlush(savedDoc);

        // File metadata
        FileMetadata meta = new FileMetadata();
        meta.setDocument(savedDoc);
        meta.setMimeType(mimeType);
        meta.setSize(file.getSize());
        meta.setHash(hash);
        meta.setUploadedAt(LocalDateTime.now());
        fileMetadataRepository.save(meta);

        // Document content placeholder
        DocumentContent content = new DocumentContent();
        content.setDocument(savedDoc);
        documentContentRepository.save(content);

        notifyDocumentUploadedAfterCommit(savedDoc);

        return savedDoc;
    }

    private void notifyDocumentUploadedAfterCommit(Document document) {
        Runnable notify = () -> {
            notificationWebSocketService.notifyDocumentUploaded(document);
            documentTextIndexingService.indexDocumentAsync(document.getDocumentId());
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
        } else {
            notify.run();
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public Optional<Document> getDocumentById(Long id) {
        Document doc = documentRepository.findByIdWithClient(id);
        return Optional.ofNullable(doc);
    }

    public List<Document> getDocumentsByClientId(Long clientId) {
        return documentRepository.findDocumentsByClientWithClient(clientId);
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
