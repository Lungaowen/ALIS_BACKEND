package za.ac.alis.legal.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.core.dto.ReportDownloadUrlDTO;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.persistence.SummaryReportRepository;

@Service
public class ReportArtifactService {

    private final PdfReportService pdfReportService;
    private final ReportStorageService reportStorageService;
    private final SummaryReportRepository summaryReportRepository;

    public ReportArtifactService(PdfReportService pdfReportService,
                                 ReportStorageService reportStorageService,
                                 SummaryReportRepository summaryReportRepository) {
        this.pdfReportService = pdfReportService;
        this.reportStorageService = reportStorageService;
        this.summaryReportRepository = summaryReportRepository;
    }

    public boolean isStorageEnabled() {
        return reportStorageService.isEnabled();
    }

    @Transactional
    public Optional<ReportDownloadUrlDTO> generateAndStore(SummaryReport report) throws IOException {
        if (!reportStorageService.isEnabled()) {
            return Optional.empty();
        }

        SummaryReport managedReport = loadReport(report);
        byte[] pdfBytes = pdfReportService.generateComplianceReportPdf(managedReport);
        ReportStorageService.StoredReport storedReport =
                reportStorageService.uploadReport(managedReport, pdfBytes);

        managedReport.setReportUrl(storedReport.reportUrl());
        summaryReportRepository.saveAndFlush(managedReport);

        return Optional.of(toDTO(managedReport, storedReport.reportUrl(),
                storedReport.signedUrl(), storedReport.expiresAt()));
    }

    @Transactional
    public ReportDownloadUrlDTO getSignedDownloadUrl(SummaryReport report) throws IOException {
        SummaryReport managedReport = loadReport(report);
        if (managedReport.getReportUrl() == null || managedReport.getReportUrl().isBlank()) {
            return generateAndStore(managedReport)
                    .orElseThrow(() -> new IllegalStateException("S3 report storage is disabled"));
        }

        ReportStorageService.SignedReport signedReport =
                reportStorageService.signReport(managedReport);
        return toDTO(managedReport, managedReport.getReportUrl(),
                signedReport.signedUrl(), signedReport.expiresAt());
    }

    private SummaryReport loadReport(SummaryReport report) {
        if (report.getReportId() == null) {
            return report;
        }
        return summaryReportRepository.findByIdWithRelations(report.getReportId())
                .orElse(report);
    }

    private ReportDownloadUrlDTO toDTO(SummaryReport report, String reportUrl,
                                       String signedUrl, java.time.Instant expiresAt) {
        return new ReportDownloadUrlDTO(report.getReportId(), reportUrl, signedUrl, expiresAt);
    }
}
