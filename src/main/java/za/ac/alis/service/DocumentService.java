package za.ac.alis.service;

import za.ac.alis.entities.Client;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import za.ac.alis.entities.FileMetadata;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.IngestionSource;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.DocumentContentRepository;
import za.ac.alis.repo.FileMetadataRepository;
import za.ac.alis.utils.FilenameSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * DOCUMENT SERVICE — HARDENED FINAL VERSION
 *
 * Fixes applied vs previous version:
 *   ✅ Filename sanitization (path traversal, null bytes, bad extensions)
 *   ✅ MIME type allowlist enforcement
 *   ✅ File size limit (10 MB default)
 *   ✅ Duplicate detection via SHA-256 hash
 *   ✅ Relative storage path (portable across OS + cloud)
 *   ✅ DB inserts BEFORE disk write (no orphaned files on DB failure)
 *   ✅ DocumentContent row created at upload (ready for AI pipeline)
 *   ✅ Full @Transactional coverage
 */
@Service
public class DocumentService {

    // ── Repositories ──────────────────────────────────────────────────────────
    private final DocumentRepository        documentRepository;
    private final ClientRepository          clientRepository;
    private final FileMetadataRepository    fileMetadataRepository;
    private final DocumentContentRepository documentContentRepository;
    private final AiPipelineService         aiPipelineService;

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String STORAGE_BASE    = "uploads";
    private static final long   MAX_FILE_BYTES  = 10 * 1024 * 1024; // 10 MB

    public DocumentService(DocumentRepository        documentRepository,
                           ClientRepository          clientRepository,
                           FileMetadataRepository    fileMetadataRepository,
                           DocumentContentRepository documentContentRepository,
                           AiPipelineService         aiPipelineService) {
        this.documentRepository        = documentRepository;
        this.clientRepository          = clientRepository;
        this.fileMetadataRepository    = fileMetadataRepository;
        this.documentContentRepository = documentContentRepository;
        this.aiPipelineService         = aiPipelineService;
    }

    // =========================================================================
    // UPLOAD DOCUMENT
    // =========================================================================
    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId) throws Exception {

        // ── STEP 1: Null / empty file guard ───────────────────────────────────
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided or file is empty.");
        }

        // ── STEP 2: File size limit ────────────────────────────────────────────
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException(
                "File too large. Maximum allowed size is 10 MB. "
                + "Received: " + (file.getSize() / 1024 / 1024) + " MB.");
        }

        // ── STEP 3: Sanitize filename + validate MIME type ─────────────────────
        //    SECURITY: prevents path traversal, null bytes, blocked extensions
        String mimeType        = file.getContentType() != null
                                    ? file.getContentType()
                                    : "application/octet-stream";
        String safeFilename    = FilenameSanitizer.sanitize(file.getOriginalFilename(), mimeType);

        // ── STEP 4: Validate client ────────────────────────────────────────────
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        // ── STEP 5: Read bytes + SHA-256 hash ─────────────────────────────────
        byte[] fileBytes = file.getBytes();
        String hash      = computeSha256(fileBytes);

        // ── STEP 6: Duplicate file detection ──────────────────────────────────
        if (fileMetadataRepository.existsByHash(hash)) {
            throw new RuntimeException(
                "This file has already been uploaded (duplicate content detected).");
        }

        // ── STEP 7: Build portable relative storage path ──────────────────────
        //    Format: uploads/{clientId}/{safeFilename}
        //    Stored as: {clientId}/{safeFilename}  (relative — no OS prefix)
        String relativePath = clientId + "/" + safeFilename;
        Path   fullPath     = Paths.get(STORAGE_BASE, relativePath);

        // ── STEP 8: Persist Document (core business record) ───────────────────
        Document doc = new Document();
        doc.setTitle(safeFilename);
        doc.setClient(client);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus(DocumentStat.PENDING);
        doc.setIngestionSource(IngestionSource.UPLOAD);
        Document savedDoc = documentRepository.save(doc);

        // ── STEP 9: Persist FileMetadata (storage tracking layer) ─────────────
        FileMetadata meta = new FileMetadata();
        meta.setDocument(savedDoc);
        meta.setMimeType(mimeType);
        meta.setSize((long) fileBytes.length);
        meta.setHash(hash);
        meta.setStoragePath(relativePath);   // ✅ relative, portable
        fileMetadataRepository.save(meta);

        // ── STEP 10: Persist DocumentContent shell (AI processing layer) ───────
        //     Nulls here — TextExtractionService fills them in Stage 1
        DocumentContent content = new DocumentContent();
        content.setDocument(savedDoc);
        content.setExtractedText(null);
        content.setEmbeddingVector(null);
        documentContentRepository.save(content);

        // ── STEP 11: Write file to disk AFTER all DB inserts ──────────────────
        //    DB first → then disk. If DB fails, no orphaned file is created.
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, fileBytes);

        // ── STEP 12: Trigger AI pipeline (runs text extraction + future stages) 
        //    NOTE: runs in same thread for now.
        //    TODO: move to @Async or message queue for production scale.
        aiPipelineService.processDocument(savedDoc.getDocumentId());

        return savedDoc;
    }

    // =========================================================================
    // SHA-256 HASH HELPER
    // =========================================================================
    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes     = digest.digest(data);
        return HexFormat.of().formatHex(hashBytes);
    }
    // Add these to DocumentService.java
public Optional<Document> getDocumentById(Long id) {
    return documentRepository.findById(id);
}

public List<Document> getDocumentsByClientId(Long clientId) {
    return documentRepository.findByClientClientId(clientId);
}

public List<Document> getAllDocuments() {
    return documentRepository.findAll();
}

public void deleteDocument(Long id) {
    documentRepository.deleteById(id);
}
}
