package za.ac.alis.service;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.repo.SummaryReportRepository;
import org.springframework.stereotype.Service;

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

    public Object findByDocumentId(Long documentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByDocumentId'");
    }

    public Object findByClientId(Long clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByClientId'");
    }
}