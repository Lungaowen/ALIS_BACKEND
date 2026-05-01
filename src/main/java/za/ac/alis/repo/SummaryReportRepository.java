package za.ac.alis.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.RiskLevel;
import za.ac.alis.projections.ReportInfoProjection;
import za.ac.alis.projections.RiskStatProjection;
import static za.ac.alis.queries.AdminDashboardQueries.FIND_RECENT_REPORTS;
import static za.ac.alis.queries.AdminDashboardQueries.RISK_DISTRIBUTION;

@Repository
public interface SummaryReportRepository extends JpaRepository<SummaryReport, Long> {

    long countByRiskLevel(RiskLevel riskLevel);

    @Query(FIND_RECENT_REPORTS)
    List<ReportInfoProjection> findRecentReports(Pageable pageable);

    @Query(RISK_DISTRIBUTION)
    List<RiskStatProjection> findRiskDistribution();

    // Fetch all reports for a document (should return 0 or 1 after the pipeline redesign)
    List<SummaryReport> findByDocumentDocumentId(Long documentId);

    @EntityGraph(attributePaths = { "document", "document.client", "client", "lawRule", "lawRule.act" })
    List<SummaryReport> findByDocumentDocumentIdOrderByGeneratedAtDesc(Long documentId);

    // Fetch the single report for a document
    @EntityGraph(attributePaths = { "document", "document.client", "client", "lawRule", "lawRule.act" })
    Optional<SummaryReport> findFirstByDocument_DocumentId(Long documentId);

    @EntityGraph(attributePaths = { "document", "document.client", "client", "lawRule", "lawRule.act" })
    Optional<SummaryReport> findById(Long reportId);

    @EntityGraph(attributePaths = { "document", "document.client", "client", "lawRule", "lawRule.act" })
    @Query("SELECT r FROM SummaryReport r WHERE r.reportId = :reportId")
    Optional<SummaryReport> findByIdWithRelations(Long reportId);

    List<SummaryReport> findByClientClientId(Long clientId);

    /**
     * Deletes any existing report(s) for the given document.
     * Called before saving a fresh report to guarantee 1-per-document.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SummaryReport r WHERE r.document.documentId = :documentId")
    void deleteByDocument_DocumentId(Long documentId);
}
