package za.ac.alis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.entities.*;
import za.ac.alis.enums.AnalysisStatus;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.repo.*;

import java.io.IOException;
import java.util.List;

@Service
public class AiPipelineService {

    private static final Logger log = LoggerFactory.getLogger(AiPipelineService.class);

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final SummaryReportRepository summaryReportRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final TextExtractionService textExtractionService;
    private final AIAnalysisService aiAnalysisService;
    private final FirebaseStorageService firebaseStorageService;
    private final LawRuleRepository lawRuleRepository;

    public AiPipelineService(DocumentRepository documentRepository,
                             DocumentContentRepository documentContentRepository,
                             SummaryReportRepository summaryReportRepository,
                             FileMetadataRepository fileMetadataRepository,
                             TextExtractionService textExtractionService,
                             AIAnalysisService aiAnalysisService,
                             FirebaseStorageService firebaseStorageService,
                             LawRuleRepository lawRuleRepository) {
        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.summaryReportRepository = summaryReportRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.textExtractionService = textExtractionService;
        this.aiAnalysisService = aiAnalysisService;
        this.firebaseStorageService = firebaseStorageService;
        this.lawRuleRepository = lawRuleRepository;
    }

    @Async
    @Transactional
    public void processDocument(Long documentId) {
        log.info("AI pipeline starting for Document ID={}", documentId);
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

            String extractedText = runTextExtraction(document);
            if (extractedText == null || extractedText.isBlank()) {
                markDocumentFailed(document, "Text extraction returned empty content");
                return;
            }

            saveExtractedText(document, extractedText);
            runAnalysisAndGenerateReport(document, extractedText);
            markDocumentCompleted(document);
            log.info("AI pipeline completed successfully for Document ID={}", documentId);
        } catch (Exception e) {
            log.error("AI pipeline failed for Document ID={}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> markDocumentFailed(doc, e.getMessage()));
        }
    }

    private String runTextExtraction(Document document) {
        log.info("  [Stage 1] Text extraction — Document ID={}", document.getDocumentId());
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
            log.error("Firebase download failed", e);
            throw new RuntimeException("Could not download file", e);
        }
        try {
            return textExtractionService.extractTextFromBytes(fileBytes, mimeType);
        } catch (IOException e) {
            log.error("Text extraction failed", e);
            throw new RuntimeException("Text extraction failed", e);
        }
    }

    private void saveExtractedText(Document document, String extractedText) {
        DocumentContent content = documentContentRepository.findByDocument(document)
                .orElseGet(() -> {
                    DocumentContent newContent = new DocumentContent();
                    newContent.setDocument(document);
                    return newContent;
                });
        content.setExtractedText(extractedText);
        documentContentRepository.save(content);
    }

    private void runAnalysisAndGenerateReport(Document document, String extractedText) {
        log.info("  [Stage 2] AI Analysis — Document ID={}", document.getDocumentId());
        List<LawRul> rules = lawRuleRepository.findAllWithAct();
        if (rules.isEmpty()) {
            log.warn("No rules found — document will be marked as failed");
            throw new RuntimeException("No law rules available");
        }
        summaryReportRepository.deleteByDocument_DocumentId(document.getDocumentId());
        SummaryReport report = aiAnalysisService.analyzeDocument(document, extractedText, rules);
        if (report == null) {
            throw new RuntimeException("AI analysis returned null report");
        }
        report.setAnalysisStatus(AnalysisStatus.COMPLETED);
        summaryReportRepository.save(report);
        log.info("  Report saved (risk={})", report.getRiskLevel());
    }

    private void markDocumentCompleted(Document document) {
        document.setStatus(DocumentStat.ANALYZED);
        documentRepository.save(document);
    }

    private void markDocumentFailed(Document document, String reason) {
        if (document != null) {
            document.setStatus(DocumentStat.FAILED);
            documentRepository.save(document);
        }
    }
}