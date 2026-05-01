package za.ac.alis.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.entities.Clause;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import za.ac.alis.entities.FileMetadata;
import za.ac.alis.entities.LawRul;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.repo.ClauseRepository;
import za.ac.alis.repo.DocumentContentRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.FileMetadataRepository;
import za.ac.alis.repo.LawRuleRepository;
import za.ac.alis.repo.SummaryReportRepository;

/**
 * AI Pipeline Orchestrator — runs asynchronously after document upload.
 *
 * Stages:
 *   1. Text Extraction   — download from Firebase, parse to plain text
 *   2. Clause Extraction — split text into searchable clause segments
 *   3. Groq AI Analysis  — ONE Groq call → ONE SummaryReport per document
 *   4. Finalize          — mark document ANALYZED
 *
 * Guarantee: exactly one {@link SummaryReport} exists per document after
 * a successful run. Any previous report is deleted before a new one is saved.
 */
@Service
public class AiPipelineService {

    private static final Logger log = LoggerFactory.getLogger(AiPipelineService.class);

    private final DocumentRepository        documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final FileMetadataRepository    fileMetadataRepository;
    private final ClauseRepository          clauseRepository;
    private final SummaryReportRepository   summaryReportRepository;
    private final LawRuleRepository         lawRuleRepository;
    private final TextExtractionService     textExtractionService;
    private final ClauseExtractionService   clauseExtractionService;
    private final AIAnalysisService         aiAnalysisService;

    public AiPipelineService(DocumentRepository documentRepository,
                             DocumentContentRepository documentContentRepository,
                             FileMetadataRepository fileMetadataRepository,
                             ClauseRepository clauseRepository,
                             SummaryReportRepository summaryReportRepository,
                             LawRuleRepository lawRuleRepository,
                             TextExtractionService textExtractionService,
                             ClauseExtractionService clauseExtractionService,
                             AIAnalysisService aiAnalysisService) {
        this.documentRepository        = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.fileMetadataRepository    = fileMetadataRepository;
        this.clauseRepository          = clauseRepository;
        this.summaryReportRepository   = summaryReportRepository;
        this.lawRuleRepository         = lawRuleRepository;
        this.textExtractionService     = textExtractionService;
        this.clauseExtractionService   = clauseExtractionService;
        this.aiAnalysisService         = aiAnalysisService;
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * Called from {@link DocumentService} after upload, and from
     * {@link za.ac.alis.controller.ComplianceController} for re-analysis.
     *
     * Runs on the {@code taskExecutor} thread pool (see AsyncConfig).
     */
    @Async("taskExecutor")
    public void processDocument(Long documentId) {
        log.info("▶ AI pipeline starting for Document ID={}", documentId);

        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.error("Document ID={} not found — pipeline aborted", documentId);
            return;
        }

        // Allow re-analysis of FAILED or ANALYZED documents; block EXTRACTED (in-progress)
        if (doc.getStatus() == DocumentStat.EXTRACTED) {
            log.warn("Document ID={} is already being processed — skipping duplicate run",
                    documentId);
            return;
        }

        // Reset to PENDING so status reflects a fresh run
        doc.setStatus(DocumentStat.PENDING);
        documentRepository.save(doc);

        try {
            // ── Stage 1 ───────────────────────────────────────────────────────
            String extractedText = runTextExtraction(doc);

            // ── Stage 2 ───────────────────────────────────────────────────────
            List<Clause> clauses = runClauseExtraction(doc, extractedText);

            // ── Stage 3 ───────────────────────────────────────────────────────
            runGroqAnalysis(doc, extractedText, clauses);

            // ── Stage 4 ───────────────────────────────────────────────────────
            doc.setStatus(DocumentStat.ANALYZED);
            documentRepository.save(doc);

            log.info("✅ AI pipeline complete — Document ID={} marked ANALYZED", documentId);

        } catch (Exception e) {
            log.error("❌ AI pipeline failed for Document ID={}", documentId, e);
            doc.setStatus(DocumentStat.FAILED);
            documentRepository.save(doc);
        }
    }

    // ── Stage 1: Text extraction ──────────────────────────────────────────────

    @Transactional
    protected String runTextExtraction(Document doc) throws Exception {
        log.info("  [Stage 1] Text extraction — Document ID={}", doc.getDocumentId());

        FileMetadata meta = fileMetadataRepository.findByDocument(doc)
                .orElseThrow(() -> new RuntimeException(
                        "FileMetadata missing for Document ID=" + doc.getDocumentId()));

        String text = textExtractionService.extractText(doc.getFileUrl(), meta.getMimeType());

        if (text == null || text.isBlank())
            throw new RuntimeException("Text extraction returned empty content");

        DocumentContent content = documentContentRepository.findByDocument(doc)
                .orElseThrow(() -> new RuntimeException(
                        "DocumentContent missing for Document ID=" + doc.getDocumentId()));

        content.setExtractedText(text);
        documentContentRepository.save(content);

        doc.setStatus(DocumentStat.EXTRACTED);
        documentRepository.save(doc);

        log.info("  [Stage 1] Complete — {} chars extracted", text.length());
        return text;
    }

    // ── Stage 2: Clause extraction ────────────────────────────────────────────

    @Transactional
    protected List<Clause> runClauseExtraction(Document doc, String extractedText) {
        log.info("  [Stage 2] Clause extraction — Document ID={}", doc.getDocumentId());

        // Remove stale clauses from any previous run
        clauseRepository.deleteByDocument(doc);

        List<Clause> clauses = clauseExtractionService.extractClauses(doc, extractedText);
        clauseRepository.saveAll(clauses);

        log.info("  [Stage 2] Complete — {} clauses saved", clauses.size());
        return clauses;
    }

    // ── Stage 3: Groq AI analysis → ONE SummaryReport ────────────────────────

    @Transactional
    protected void runGroqAnalysis(Document doc, String extractedText, List<Clause> clauses) {
        log.info("  [Stage 3] Groq AI analysis — Document ID={}", doc.getDocumentId());

        // Delete any previous report for this document (ensure 1-per-document)
        summaryReportRepository.deleteByDocument_DocumentId(doc.getDocumentId());

        // Load all seeded law rules
        List<LawRul> allRules = lawRuleRepository.findAllWithAct();

        if (allRules.isEmpty()) {
            log.warn("  [Stage 3] No law rules in DB — Groq will use general compliance knowledge");
        }

        // ONE Groq call → ONE SummaryReport (unsaved)
        SummaryReport report = aiAnalysisService.analyzeDocument(doc, extractedText, allRules);

        // Persist the single report
        summaryReportRepository.save(report);

        log.info("  [Stage 3] Complete — 1 SummaryReport saved (risk={})", report.getRiskLevel());
    }
}
