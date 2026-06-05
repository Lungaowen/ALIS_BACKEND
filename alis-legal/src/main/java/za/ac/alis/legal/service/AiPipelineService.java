package za.ac.alis.legal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import za.ac.alis.core.persistence.*;
import za.ac.alis.core.enums.AnalysisStatus;
import za.ac.alis.core.enums.DocumentStat;
import za.ac.alis.legal.persistence.DocumentContentRepository;
import za.ac.alis.legal.persistence.DocumentRepository;
import za.ac.alis.legal.persistence.FileMetadataRepository;
import za.ac.alis.legal.persistence.LawRuleRepository;
import za.ac.alis.legal.persistence.SummaryReportRepository;
import za.ac.alis.ai.service.AIAnalysisService;
import za.ac.alis.ai.service.TextExtractionService;

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
    private final ObjectProvider<AIAnalysisService> aiAnalysisService;
    private final FirebaseStorageService firebaseStorageService;
    private final LawRuleRepository lawRuleRepository;
    private final NotificationWebSocketService notificationWebSocketService;
    private final DocumentTextIndexingService documentTextIndexingService;
    private final ReportArtifactService reportArtifactService;

    public AiPipelineService(DocumentRepository documentRepository,
                             DocumentContentRepository documentContentRepository,
                             SummaryReportRepository summaryReportRepository,
                             FileMetadataRepository fileMetadataRepository,
                             TextExtractionService textExtractionService,
                             ObjectProvider<AIAnalysisService> aiAnalysisService,
                             FirebaseStorageService firebaseStorageService,
                             LawRuleRepository lawRuleRepository,
                             NotificationWebSocketService notificationWebSocketService,
                             DocumentTextIndexingService documentTextIndexingService,
                             ReportArtifactService reportArtifactService) {
        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.summaryReportRepository = summaryReportRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.textExtractionService = textExtractionService;
        this.aiAnalysisService = aiAnalysisService;
        this.firebaseStorageService = firebaseStorageService;
        this.lawRuleRepository = lawRuleRepository;
        this.notificationWebSocketService = notificationWebSocketService;
        this.documentTextIndexingService = documentTextIndexingService;
        this.reportArtifactService = reportArtifactService;
    }

    @Async
    @Transactional
    public void processDocument(Long documentId) {
        log.info("AI pipeline starting for Document ID={}", documentId);
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

            String extractedText = documentTextIndexingService.ensureIndexedText(document);
            if (extractedText == null || extractedText.isBlank()) {
                String reason = "Text extraction returned empty content";
                markDocumentFailed(document, reason);
                notifyAnalysisFailedAfterCommit(document, reason);
                return;
            }

            saveExtractedText(document, extractedText);
            SummaryReport report = runAnalysisAndGenerateReport(document, extractedText);
            markDocumentCompleted(document);
            notifyReportReadyAfterCommit(report);
            log.info("AI pipeline completed successfully for Document ID={}", documentId);
        } catch (Exception e) {
            log.error("AI pipeline failed for Document ID={}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                markDocumentFailed(doc, e.getMessage());
                notifyAnalysisFailedAfterCommit(doc, e.getMessage());
            });
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

    private SummaryReport runAnalysisAndGenerateReport(Document document, String extractedText) {
        log.info("  [Stage 2] AI Analysis — Document ID={}", document.getDocumentId());
        List<LawRul> rules = lawRuleRepository.findAllWithAct();
        if (rules.isEmpty()) {
            log.warn("No rules found — document will be marked as failed");
            throw new RuntimeException("No law rules available");
        }
        summaryReportRepository.deleteByDocument_DocumentId(document.getDocumentId());
        AIAnalysisService analysisService = aiAnalysisService.getIfAvailable();
        if (analysisService == null) {
            throw new RuntimeException("AI analysis is not configured (alis.ai.groq.key missing)");
        }
        SummaryReport report = analysisService.analyzeDocument(document, extractedText, rules);
        if (report == null) {
            throw new RuntimeException("AI analysis returned null report");
        }
        if (report.getAnalysisStatus() == null) {
            report.setAnalysisStatus(AnalysisStatus.COMPLETED);
        }
        report = summaryReportRepository.saveAndFlush(report);
        generateReportArtifact(report);
        log.info("  Report saved (risk={})", report.getRiskLevel());
        if (report.getAnalysisStatus() == AnalysisStatus.FAILED) {
            throw new RuntimeException(report.getAiExplanation());
        }
        return report;
    }

    private void generateReportArtifact(SummaryReport report) {
        if (!reportArtifactService.isStorageEnabled()) {
            return;
        }
        try {
            reportArtifactService.generateAndStore(report);
        } catch (Exception e) {
            throw new RuntimeException("PDF report generation/upload failed", e);
        }
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

    private void notifyReportReadyAfterCommit(SummaryReport report) {
        runAfterCommit(() -> notificationWebSocketService.notifyReportReady(report));
    }

    private void notifyAnalysisFailedAfterCommit(Document document, String reason) {
        runAfterCommit(() -> notificationWebSocketService.notifyAnalysisFailed(document, reason));
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
