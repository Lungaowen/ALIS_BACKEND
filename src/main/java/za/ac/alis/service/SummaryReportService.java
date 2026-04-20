package za.ac.alis.service;

import org.springframework.stereotype.Service;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.repo.SummaryReportRepository;

import java.util.List;
import java.util.Optional;

@Service
public class SummaryReportService {

    private final SummaryReportRepository summaryReportRepository;

    public SummaryReportService(SummaryReportRepository summaryReportRepository) {
        this.summaryReportRepository = summaryReportRepository;
    }

    public Optional<SummaryReport> findById(Long id) {
        return summaryReportRepository.findById(id);
    }

    public SummaryReport save(SummaryReport report) {
        return summaryReportRepository.save(report);
    }

    public void deleteById(Long id) {
        summaryReportRepository.deleteById(id);
    }

    public List<SummaryReport> findByDocumentId(Long documentId) {
        return summaryReportRepository.findByDocumentDocumentId(documentId);
    }

    public List<SummaryReport> findByClientId(Long clientId) {
        return summaryReportRepository.findByClientClientId(clientId);
    }
}