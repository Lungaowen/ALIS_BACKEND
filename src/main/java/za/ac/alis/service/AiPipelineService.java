package za.ac.alis.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import za.ac.alis.entities.FileMetadata;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.repo.DocumentContentRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.FileMetadataRepository;

import java.time.LocalDateTime;

/**
 * AI PIPELINE — ORCHESTRATOR
 *
 * This service drives the full AI processing pipeline for a document.
 * It is triggered AFTER a document is uploaded.
 *
 * Pipeline stages:
 *
 *   [1] PENDING   → Text extraction from file (TextExtractionService)
 *   [2] EXTRACTED → Clause splitting        (ClauseExtractionService)  ← coming soon
 *   [3] ANALYSED  → Rule matching           (RuleMatchingService)      ← coming soon
 *   [4] COMPLETE  → SummaryReport generated (SummaryReportService)     ← coming soon
 *
 * Each stage updates Document.status so the frontend can show real-time progress.
 *
 * Call this from your controller or a background job (@Async / scheduler).
 */
@Service
public class AiPipelineService {

    private final DocumentRepository        documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final FileMetadataRepository    fileMetadataRepository;
    private final TextExtractionService     textExtractionService;

    public AiPipelineService(
            DocumentRepository        documentRepository,
            DocumentContentRepository documentContentRepository,
            FileMetadataRepository    fileMetadataRepository,
            TextExtractionService     textExtractionService) {
        this.documentRepository        = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.fileMetadataRepository    = fileMetadataRepository;
        this.textExtractionService     = textExtractionService;
    }

    // =========================================================================
    // ENTRY POINT — call this after uploadDocument() completes
    // =========================================================================
    @Transactional
    public void processDocument(Long documentId) {
        // ── Load document ──────────────────────────────────────────────────────
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // ── Guard: skip if already processed ──────────────────────────────────
        if (doc.getStatus() != DocumentStat.PENDING) {
            System.out.println("[AI Pipeline] Skipping document " + documentId
                + " — status is already: " + doc.getStatus());
            return;
        }

        try {
            // ── STAGE 1: Text Extraction ───────────────────────────────────────
            runTextExtraction(doc);

            // ── STAGE 2: Clause Extraction (stub — wire in ClauseExtractionService later)
            // runClauseExtraction(doc);

            // ── STAGE 3: Rule Matching (stub — wire in RuleMatchingService later)
            // runRuleMatching(doc);

            // ── STAGE 4: Summary Report (stub — wire in SummaryReportService later)
            // runSummaryReport(doc);

            // ── Mark complete ──────────────────────────────────────────────────
            doc.setStatus(DocumentStat.ANALYSED);
            documentRepository.save(doc);

            System.out.println("[AI Pipeline] ✅ Document " + documentId + " fully processed.");

        } catch (Exception e) {
            // ── Mark failed — don't crash the whole app ────────────────────────
            doc.setStatus(DocumentStat.FAILED);
            documentRepository.save(doc);
            System.err.println("[AI Pipeline] ❌ Failed for document " + documentId
                + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // STAGE 1 — TEXT EXTRACTION
    // =========================================================================
    private void runTextExtraction(Document doc) throws Exception {
    System.out.println("[AI Pipeline] Stage 1 — Extracting text for document: " + doc.getDocumentId());

    FileMetadata meta = fileMetadataRepository.findByDocument(doc)  // Fix 5 enables this
            .orElseThrow(() -> new RuntimeException(
                "FileMetadata not found for document: " + doc.getDocumentId()));

    String extractedText = textExtractionService.extractText(
            meta.getStoragePath(),
            meta.getMimeType()
    );

    if (extractedText == null || extractedText.isBlank()) {
        throw new RuntimeException("Text extraction returned empty content.");
    }

    DocumentContent content = documentContentRepository.findByDocument(doc)
            .orElseThrow(() -> new RuntimeException(
                "DocumentContent row missing for document: " + doc.getDocumentId()));

    content.setExtractedText(extractedText);
    documentContentRepository.save(content);   // ✅ Fix 6: save content, not doc

    doc.setStatus(DocumentStat.EXTRACTED);     // ✅ Fix 7: EXTRACTED now exists in enum
    documentRepository.save(doc);              // ✅ save doc separately via documentRepository

    System.out.println("[AI Pipeline] ✅ Stage 1 complete. Characters: " + extractedText.length());
}
}
