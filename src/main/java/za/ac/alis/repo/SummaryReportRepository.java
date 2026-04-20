package za.ac.alis.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.RiskLevel;
import za.ac.alis.projections.ReportInfoProjection;
import za.ac.alis.projections.RiskStatProjection;

import java.util.List;

import static za.ac.alis.queries.AdminDashboardQueries.*;

@Repository
public interface SummaryReportRepository extends JpaRepository<SummaryReport, Long> {

    long countByRiskLevel(RiskLevel riskLevel);

    @Query(FIND_RECENT_REPORTS)
    List<ReportInfoProjection> findRecentReports(Pageable pageable);

    @Query(RISK_DISTRIBUTION)
    List<RiskStatProjection> findRiskDistribution();

    List<SummaryReport> findByDocumentDocumentId(Long documentId);
    List<SummaryReport> findByClientClientId(Long clientId);
}
