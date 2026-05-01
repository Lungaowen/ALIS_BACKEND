package za.ac.alis.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.dto.*;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.RiskLevel;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.SummaryReportRepository;
import za.ac.alis.projections.ClientActivityProjection;
import za.ac.alis.projections.DocumentInfoProjection;
import za.ac.alis.projections.ReportInfoProjection;
import za.ac.alis.projections.RoleStatProjection;
import za.ac.alis.projections.RiskStatProjection;
import za.ac.alis.projections.MonthlyCountProjection;


import za.ac.alis.dto.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final ClientRepository        clientRepository;
    private final DocumentRepository      documentRepository;
    private final SummaryReportRepository summaryReportRepository;

    public AdminDashboardService(ClientRepository clientRepository,
                                 DocumentRepository documentRepository,
                                 SummaryReportRepository summaryReportRepository) {
        this.clientRepository        = clientRepository;
        this.documentRepository      = documentRepository;
        this.summaryReportRepository = summaryReportRepository;
    }

    // ─────────────────────────────────────────────
    // SINGLE CALL → full dashboard payload
    // ─────────────────────────────────────────────

    public AdminDashboardResponseDTO getDashboard() {
        AdminDashboardResponseDTO response = new AdminDashboardResponseDTO();

        response.setStats(buildStats());
        response.setClients(buildClientActivity());
        response.setRecentDocuments(buildRecentDocuments());
        response.setReports(buildRecentReports());
        response.setRoleDistribution(buildRoleDistribution());
        response.setRiskDistribution(buildRiskDistribution());
        response.setUploadTrend(buildUploadTrend());

        return response;
    }

    // ─────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────

    private DashboardStatsDTO buildStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        stats.setTotalClients(clientRepository.count());
        stats.setTotalDocuments(documentRepository.count());
        stats.setTotalReports(summaryReportRepository.count());
        stats.setActiveClients(documentRepository.countActiveClients());
        stats.setPendingDocuments(documentRepository.countByStatus(DocumentStat.PENDING));
        stats.setFailedDocuments(documentRepository.countByStatus(DocumentStat.FAILED));
        stats.setProcessedDocuments(documentRepository.countByStatus(DocumentStat.ANALYZED));
        stats.setHighRiskReports(summaryReportRepository.countByRiskLevel(RiskLevel.HIGH));

        return stats;
    }

    // ─────────────────────────────────────────────
    // CLIENT ACTIVITY — projection → DTO via from()
    // ─────────────────────────────────────────────

    private List<ClientActivityDTO> buildClientActivity() {
        return clientRepository
                .findClientActivities(PageRequest.of(0, 20))
                .stream()
                .map(ClientActivityDTO::from)
                .toList();
    }

    // ─────────────────────────────────────────────
    // RECENT DOCUMENTS — projection → DTO via from()
    // ─────────────────────────────────────────────

    private List<DocumentInfoDTO> buildRecentDocuments() {
        return documentRepository
                .findRecentDocuments(PageRequest.of(0, 10))
                .stream()
                .map(DocumentInfoDTO::from)
                .toList();
    }

    // ─────────────────────────────────────────────
    // RECENT REPORTS — projection → DTO via from()
    // ─────────────────────────────────────────────

   private List<ReportInfoDTO> buildRecentReports() {
    return summaryReportRepository.findRecentReports(PageRequest.of(0, 10))
            .stream()
            .map(ReportInfoDTO::fromProjection)
            .collect(Collectors.toList());
}
    // ─────────────────────────────────────────────
    // ROLE DISTRIBUTION — projection → DTO via from()
    // ─────────────────────────────────────────────

    private List<RoleStatDTO> buildRoleDistribution() {
        return clientRepository.findRoleDistribution()
                .stream()
                .map(RoleStatDTO::from)
                .toList();
    }

    // ─────────────────────────────────────────────
    // RISK DISTRIBUTION — projection → DTO via from()
    // ─────────────────────────────────────────────

    private List<RiskStatDTO> buildRiskDistribution() {
        return summaryReportRepository.findRiskDistribution()
                .stream()
                .map(RiskStatDTO::from)
                .toList();
    }

    // ─────────────────────────────────────────────
    // UPLOAD TREND — projection → DTO via from()
    // ─────────────────────────────────────────────

    private List<MonthlyUploadDTO> buildUploadTrend() {
        return documentRepository
                .findMonthlyUploads(LocalDateTime.now().minusMonths(12))
                .stream()
                .map(MonthlyUploadDTO::from)
                .toList();
    }
}
