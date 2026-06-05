package za.ac.alis.legal.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.ai.service.TextExtractionService;
import za.ac.alis.core.enums.DocumentStat;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.DocumentChunk;
import za.ac.alis.core.persistence.DocumentContent;
import za.ac.alis.core.persistence.FileMetadata;
import za.ac.alis.legal.persistence.DocumentChunkRepository;
import za.ac.alis.legal.persistence.DocumentContentRepository;
import za.ac.alis.legal.persistence.DocumentRepository;
import za.ac.alis.legal.persistence.FileMetadataRepository;

@Service
public class DocumentTextIndexingService {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentTextIndexingService.class);

    private static final int CHUNK_WORDS = 450;
    private static final int CHUNK_OVERLAP_WORDS = 80;

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final TextExtractionService textExtractionService;
    private final NotificationWebSocketService notificationWebSocketService;

    public DocumentTextIndexingService(DocumentRepository documentRepository,
                                       DocumentContentRepository documentContentRepository,
                                       DocumentChunkRepository documentChunkRepository,
                                       FileMetadataRepository fileMetadataRepository,
                                       FirebaseStorageService firebaseStorageService,
                                       TextExtractionService textExtractionService,
                                       NotificationWebSocketService notificationWebSocketService) {
        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.textExtractionService = textExtractionService;
        this.notificationWebSocketService = notificationWebSocketService;
    }

    @Async
    @Transactional
    public void indexDocumentAsync(Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            ensureIndexedText(document);
            notificationWebSocketService.notifyDocumentIndexed(document);
            log.info("Document indexed for RAG search: Document ID={}", documentId);
        } catch (Exception e) {
            log.error("Document indexing failed for Document ID={}", documentId, e);
            documentRepository.findById(documentId).ifPresent(document -> {
                document.setStatus(DocumentStat.FAILED);
                documentRepository.save(document);
                notificationWebSocketService.notifyAnalysisFailed(document, e.getMessage());
            });
        }
    }

    @Transactional
    public String ensureIndexedText(Document document) {
        DocumentContent content = documentContentRepository.findByDocument(document)
                .orElseGet(() -> {
                    DocumentContent newContent = new DocumentContent();
                    newContent.setDocument(document);
                    return newContent;
                });

        String extractedText = content.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            extractedText = extractText(document);
            if (extractedText == null || extractedText.isBlank()) {
                throw new RuntimeException("Text extraction returned empty content");
            }
            content.setExtractedText(extractedText);
            documentContentRepository.save(content);
        }

        List<DocumentChunk> existingChunks =
                documentChunkRepository.findByDocumentDocumentIdOrderByChunkIndexAsc(
                        document.getDocumentId());
        if (existingChunks.isEmpty()) {
            saveChunks(document, extractedText);
        }

        if (document.getStatus() == DocumentStat.PENDING) {
            document.setStatus(DocumentStat.EXTRACTED);
            documentRepository.save(document);
        }

        return extractedText;
    }

    private String extractText(Document document) {
        String objectPath = document.getFilePath();
        if (objectPath == null || objectPath.isBlank()) {
            throw new RuntimeException("Document has no file path: " + document.getDocumentId());
        }

        String mimeType = fileMetadataRepository.findByDocument(document)
                .map(FileMetadata::getMimeType)
                .orElse("application/pdf");

        byte[] fileBytes;
        try {
            fileBytes = firebaseStorageService.downloadFile(objectPath);
        } catch (Exception e) {
            throw new RuntimeException("Could not download file", e);
        }

        try {
            return textExtractionService.extractTextFromBytes(fileBytes, mimeType);
        } catch (IOException e) {
            throw new RuntimeException("Text extraction failed", e);
        }
    }

    private void saveChunks(Document document, String text) {
        documentChunkRepository.deleteByDocument(document);
        List<String> chunks = chunkText(text);
        List<DocumentChunk> entities = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setChunkText(chunkText);
            chunk.setTokenCount(countWords(chunkText));
            entities.add(chunk);
        }

        documentChunkRepository.saveAll(entities);
    }

    private List<String> chunkText(String text) {
        String cleaned = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) {
            return List.of();
        }

        List<String> words = Arrays.asList(cleaned.split("\\s+"));
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < words.size()) {
            int end = Math.min(start + CHUNK_WORDS, words.size());
            chunks.add(String.join(" ", words.subList(start, end)));
            if (end == words.size()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP_WORDS, start + 1);
        }

        return chunks;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
