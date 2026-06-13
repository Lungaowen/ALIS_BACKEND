package za.ac.alis.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.repo.SummaryReportRepository;

/**
 * Service wrapper for SummaryReport persistence.
 *
 * Note: the pipeline guarantees at most ONE SummaryReport per document.
 * {@link #findOneByDocumentId(Long)} is the preferred fetch method.
 */
@Service
public class SummaryReportService {

    private final SummaryReportRepository summaryReportRepository;

    public SummaryReportService(SummaryReportRepository summaryReportRepository) {
        this.summaryReportRepository = summaryReportRepository;
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

    public void deleteById(Long id) {
        summaryReportRepository.deleteById(id);
    }
}
