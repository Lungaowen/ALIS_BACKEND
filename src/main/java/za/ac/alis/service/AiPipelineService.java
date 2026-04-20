package za.ac.alis.service;

import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import za.ac.alis.entities.FileMetadata;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.repo.DocumentContentRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI PIPELINE ORCHESTRATOR - Production Ready
 *
 * Clean separation of concerns + Async processing + Proper logging
 */
@Service
public class AiPipelineService {

    private static final Logger log = LoggerFactory.getLogger(AiPipelineService.class);

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final TextExtractionService textExtractionService;

    // Future services (uncomment when ready)
    // private final ClauseExtractionService clauseExtractionService;
    // private final RuleMatchingService ruleMatchingService;
    // private final SummaryReportService summaryReportService;

    public AiPipelineService(
            DocumentRepository documentRepository,
            DocumentContentRepository documentContentRepository,
            FileMetadataRepository fileMetadataRepository,
            TextExtractionService textExtractionService) {

        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.textExtractionService = textExtractionService;
    }

    /**
     * Main entry point — called from DocumentService after upload
     * Runs asynchronously to not block the upload response
     */
    @Async
    @Transactional
    public void processDocument(Long documentId) {
        log.info("Starting AI pipeline for document ID: {}", documentId);

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // Skip if not pending
        if (doc.getStatus() != DocumentStat.PENDING) {
            log.warn("Skipping document {} - status is already: {}", documentId, doc.getStatus());
            return;
        }

        try {
            // STAGE 1: Text Extraction (using Supabase URL)
            runTextExtraction(doc);

            // TODO: Add more stages when ready
            // runClauseExtraction(doc);
            // runRuleMatching(doc);
            // runSummaryReportGeneration(doc);

            // Final status
            doc.setStatus(DocumentStat.ANALYZED);
            documentRepository.save(doc);

            log.info("✅ AI Pipeline completed successfully for document: {}", documentId);

        } catch (Exception e) {
            log.error("❌ AI Pipeline failed for document: {}", documentId, e);

            doc.setStatus(DocumentStat.FAILED);
            documentRepository.save(doc);
        }
    }

    /**
     * STAGE 1: Text Extraction
     */
    private void runTextExtraction(Document doc) throws Exception {
        log.info("Stage 1 - Text Extraction started for document: {}", doc.getDocumentId());

        // Get FileMetadata (Document owns the relationship, so it's safe)
        FileMetadata meta = fileMetadataRepository.findByDocument(doc)
                .orElseThrow(() -> new RuntimeException("FileMetadata not found for document: " + doc.getDocumentId()));

        // Extract text using Supabase public URL
        String extractedText = textExtractionService.extractText(
                doc.getFileUrl(),
                meta.getMimeType()
        );

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new RuntimeException("Text extraction returned empty content");
        }

        // Update DocumentContent
        DocumentContent content = documentContentRepository.findByDocument(doc)
                .orElseThrow(() -> new RuntimeException("DocumentContent not found for document: " + doc.getDocumentId()));

        content.setExtractedText(extractedText);
        documentContentRepository.save(content);

        // Update status
        doc.setStatus(DocumentStat.EXTRACTED);
        documentRepository.save(doc);

        log.info("Stage 1 completed - Extracted {} characters for document {}", 
                extractedText.length(), doc.getDocumentId());
    }

    // ===================================================================
    // FUTURE STAGES (Uncomment and wire services when ready)
    // ===================================================================

    /*
    private void runClauseExtraction(Document doc) {
        log.info("Stage 2 - Clause Extraction for document: {}", doc.getDocumentId());
        // clauseExtractionService.extractClauses(doc);
        doc.setStatus(DocumentStat.CLAUSE_EXTRACTED);
        documentRepository.save(doc);
    }

    private void runRuleMatching(Document doc) {
        log.info("Stage 3 - Rule Matching for document: {}", doc.getDocumentId());
        // ruleMatchingService.matchRules(doc);
    }

    private void runSummaryReportGeneration(Document doc) {
        log.info("Stage 4 - Summary Report Generation (Gemini) for document: {}", doc.getDocumentId());
        // summaryReportService.generateReport(doc);
        doc.setStatus(DocumentStat.ANALYZED);
        documentRepository.save(doc);
    }
    */
}