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

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * DOCUMENT SERVICE — FINAL VERSION WITH SUPABASE STORAGE
 */
@Service
public class DocumentService {

    private final DocumentRepository        documentRepository;
    private final ClientRepository          clientRepository;
    private final FileMetadataRepository    fileMetadataRepository;
    private final DocumentContentRepository documentContentRepository;
    private final StorageService            storageService;           // ← Supabase storage
    private final AiPipelineService         aiPipelineService;

    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

    public DocumentService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository,
            FileMetadataRepository fileMetadataRepository,
            DocumentContentRepository documentContentRepository,
            StorageService storageService,
            AiPipelineService aiPipelineService) {

        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.documentContentRepository = documentContentRepository;
        this.storageService = storageService;
        this.aiPipelineService = aiPipelineService;
    }

    // =========================================================================
    // MAIN UPLOAD METHOD — NOW USES SUPABASE
    // =========================================================================
    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId) throws Exception {

        // ── 1. Basic validations ─────────────────────────────────────────────
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided or file is empty.");
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File too large. Maximum allowed is 10 MB.");
        }

        // ── 2. Sanitize filename + validate MIME ─────────────────────────────
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String safeFilename = FilenameSanitizer.sanitize(file.getOriginalFilename(), mimeType);

        // ── 3. Validate client ───────────────────────────────────────────────
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        // ── 4. Compute SHA-256 hash for duplicate detection ──────────────────
        byte[] fileBytes = file.getBytes();
        String hash = computeSha256(fileBytes);

        if (fileMetadataRepository.existsByHash(hash)) {
            throw new RuntimeException("This file has already been uploaded (duplicate detected).");
        }

        // ── 5. Create Document record first (DB-first strategy) ──────────────
        Document doc = new Document();
        doc.setTitle(safeFilename);
        doc.setClient(client);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus(DocumentStat.PENDING);
        doc.setIngestionSource(IngestionSource.UPLOAD);

        Document savedDoc = documentRepository.save(doc);

        // ── 6. Upload file to Supabase using StorageService ──────────────────
        String fileUrl = storageService.uploadFile(file, clientId);   // Returns public URL

        // ── 7. Create FileMetadata ───────────────────────────────────────────
        FileMetadata meta = new FileMetadata();
        meta.setDocument(savedDoc);
        meta.setMimeType(mimeType);
        meta.setSize(file.getSize());
        meta.setHash(hash);
        // We store the relative path if needed, but URL is the source of truth
        fileMetadataRepository.save(meta);

        // ── 8. Create DocumentContent shell for AI pipeline ──────────────────
        DocumentContent content = new DocumentContent();
        content.setDocument(savedDoc);
        documentContentRepository.save(content);

        // ── 9. Update Document with Supabase URL ─────────────────────────────
        savedDoc.setFileUrl(fileUrl);
        documentRepository.save(savedDoc);

        // ── 10. Trigger AI processing (text extraction + analysis) ───────────
        aiPipelineService.processDocument(savedDoc.getDocumentId());

        return savedDoc;
    }

    // =========================================================================
    // HELPER: SHA-256
    // =========================================================================
    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        return HexFormat.of().formatHex(hashBytes);
    }

    // =========================================================================
    // BASIC CRUD (for completeness)
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