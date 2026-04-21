package za.ac.alis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.ac.alis.entities.*;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.IngestionSource;
import za.ac.alis.repo.*;
import za.ac.alis.utils.FilenameSanitizer;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final DocumentContentRepository documentContentRepository;
    private final FirebaseStorageService firebaseStorageService;   // ✅ Firebase
    private final AiPipelineService aiPipelineService;

    public DocumentService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository,
            FileMetadataRepository fileMetadataRepository,
            DocumentContentRepository documentContentRepository,
            FirebaseStorageService firebaseStorageService,
            AiPipelineService aiPipelineService) {

        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.documentContentRepository = documentContentRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.aiPipelineService = aiPipelineService;
    }

    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId) throws Exception {
        // 1. Validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }

        String mimeType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String safeFilename = FilenameSanitizer.sanitize(originalFilename, mimeType);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        // 2. Compute SHA-256 hash while streaming (no memory double-loading)
        String hash;
        try (InputStream is = file.getInputStream()) {
            hash = computeSha256(is);
        }

        // 3. Duplicate check
        if (fileMetadataRepository.existsByHash(hash)) {
            throw new RuntimeException("Duplicate file detected. This document has already been uploaded.");
        }

        // 4. Create Document record
        Document doc = new Document();
        doc.setTitle(safeFilename);
        doc.setClient(client);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus(DocumentStat.PENDING);
        doc.setIngestionSource(IngestionSource.MANUAL);   // ✅ Fixed enum

        Document savedDoc = documentRepository.save(doc);
        log.info("Created document record ID {} for client {}", savedDoc.getDocumentId(), clientId);

        // 5. Upload to Firebase Storage (streaming)
        String fileUrl;
        try (InputStream is = file.getInputStream()) {
            fileUrl = firebaseStorageService.uploadFile(is, safeFilename, mimeType, clientId);
        } catch (Exception e) {
            log.error("Firebase upload failed for document {}", savedDoc.getDocumentId(), e);
            savedDoc.setStatus(DocumentStat.FAILED);
            documentRepository.save(savedDoc);
            throw new RuntimeException("Failed to upload file to cloud storage", e);
        }
        savedDoc.setFileUrl(fileUrl);
        documentRepository.save(savedDoc);

        // 6. Save FileMetadata
        FileMetadata meta = new FileMetadata();
        meta.setDocument(savedDoc);
        meta.setMimeType(mimeType);
        meta.setSize(file.getSize());
        meta.setHash(hash);
        fileMetadataRepository.save(meta);

        // 7. Create empty DocumentContent for AI pipeline
        DocumentContent content = new DocumentContent();
        content.setDocument(savedDoc);
        documentContentRepository.save(content);

        // 8. Trigger AI processing (async, with failure handling)
        try {
            //aiPipelineService.processDocument(savedDoc.getDocumentId());
            log.info("AI pipeline triggered for document {}", savedDoc.getDocumentId());
        } catch (Exception e) {
            log.error("AI pipeline failed to start for document {}", savedDoc.getDocumentId(), e);
            savedDoc.setStatus(DocumentStat.FAILED);
            documentRepository.save(savedDoc);
        }

        return savedDoc;
    }

    /**
     * Computes SHA-256 hash from an InputStream without loading the entire file into memory.
     */
    private String computeSha256(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();
        return HexFormat.of().formatHex(hashBytes);
    }

    // =========================================================================
    // BASIC CRUD
    // =========================================================================
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public List<Document> getDocumentsByClientId(Long clientId) {
        return documentRepository.findByClient_ClientId(clientId);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}