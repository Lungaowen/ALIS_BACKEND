package za.ac.alis.legal.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.persistence.SummaryReportRepository;

/**
 * Service wrapper for SummaryReport persistence.
 *
 * Note: the pipeline guarantees at most ONE SummaryReport per document.
 * {@link #findOneByDocumentId(Long)} is the preferred fetch method.
 */
@Service
public class SummaryReportService {

    private final SummaryReportRepository summaryReportRepository;
    private final ReportArtifactService reportArtifactService;

    public SummaryReportService(SummaryReportRepository summaryReportRepository) {
        this(summaryReportRepository, null);
    }

    @Autowired
    public SummaryReportService(SummaryReportRepository summaryReportRepository,
                                ReportArtifactService reportArtifactService) {
        this.summaryReportRepository = summaryReportRepository;
        this.reportArtifactService = reportArtifactService;
    }

    public Optional<SummaryReport> findById(Long id) {
        return summaryReportRepository.findById(id);
    }

    /**
     * Returns the single compliance report for a document (0 or 1 result).
     */
    public Optional<SummaryReport> findOneByDocumentId(Long documentId) {
        return summaryReportRepository.findFirstByDocument_DocumentId(documentId);
    }

    /**
     * Returns a list — will always have 0 or 1 elements after pipeline redesign.
     * Kept for backward compatibility with existing controller code.
     */
    public List<SummaryReport> findByDocumentId(Long documentId) {
        return summaryReportRepository.findByDocumentDocumentId(documentId);
    }

    public List<SummaryReport> findByDocumentIdWithRelations(Long documentId) {
        return summaryReportRepository.findByDocumentDocumentIdOrderByGeneratedAtDesc(documentId);
    }

    public List<SummaryReport> findByClientId(Long clientId) {
        return summaryReportRepository.findByClientClientId(clientId);
    }

    public SummaryReport save(SummaryReport report) {
        return summaryReportRepository.save(report);
    }

    @Transactional
    public SummaryReport updateAiContent(Long reportId, String aiExplanation, String aiRecommendation) {
        SummaryReport report = summaryReportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        if (aiExplanation != null) {
            report.setAiExplanation(aiExplanation);
        }
        if (aiRecommendation != null) {
            report.setAiRecommendation(aiRecommendation);
        }

        SummaryReport saved = summaryReportRepository.saveAndFlush(report);
        regenerateReportArtifact(saved);
        return summaryReportRepository.findByIdWithRelations(saved.getReportId()).orElse(saved);
    }

    public void deleteById(Long id) {
        summaryReportRepository.deleteById(id);
    }

    private void regenerateReportArtifact(SummaryReport report) {
        if (reportArtifactService == null || !reportArtifactService.isStorageEnabled()) {
            return;
        }
        try {
            reportArtifactService.generateAndStore(report);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to regenerate report PDF", e);
        }
    }
}
