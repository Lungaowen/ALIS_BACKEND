package za.ac.alis.legal.persistence.search;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import za.ac.alis.core.persistence.SummaryReport;

public interface ReportSearchRepository extends Repository<SummaryReport, Long> {

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            )
            SELECT
                sr.report_id AS "reportId",
                CAST(sr.risk_level AS text) AS "riskLevel",
                CAST(sr.analysis_status AS text) AS "analysisStatus",
                sr.ai_recommendation AS "aiRecommendation",
                sr.ai_explanation AS "aiExplanation",
                sr.document_id AS "documentId",
                d.title AS "documentTitle",
                sr.client_id AS "clientId",
                sr.generated_at AS "generatedAt",
                CAST(ts_rank(sr.search_vector, search_query.value) AS double precision) AS "rank"
            FROM summary_report sr
            JOIN document d ON d.document_id = sr.document_id
            CROSS JOIN search_query
            WHERE
                sr.search_vector @@ search_query.value
                AND (CAST(:clientId AS bigint) IS NULL
                    OR sr.client_id = CAST(:clientId AS bigint))
            ORDER BY "rank" DESC, sr.generated_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ReportSearchProjection> searchReports(
            @Param("query") String query,
            @Param("clientId") Long clientId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            )
            SELECT COUNT(*)
            FROM summary_report sr
            CROSS JOIN search_query
            WHERE
                sr.search_vector @@ search_query.value
                AND (CAST(:clientId AS bigint) IS NULL
                    OR sr.client_id = CAST(:clientId AS bigint))
            """, nativeQuery = true)
    long countReportSearch(
            @Param("query") String query,
            @Param("clientId") Long clientId);

    @Query(value = """
            SELECT
                sr.report_id AS "reportId",
                CAST(sr.risk_level AS text) AS "riskLevel",
                CAST(sr.analysis_status AS text) AS "analysisStatus",
                sr.ai_recommendation AS "aiRecommendation",
                sr.ai_explanation AS "aiExplanation",
                sr.document_id AS "documentId",
                d.title AS "documentTitle",
                sr.client_id AS "clientId",
                sr.generated_at AS "generatedAt",
                CAST(0 AS double precision) AS "rank"
            FROM summary_report sr
            JOIN document d ON d.document_id = sr.document_id
            WHERE
                sr.client_id = :clientId
                AND sr.risk_level = CAST(:riskLevel AS risk_level)
            ORDER BY sr.generated_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ReportSearchProjection> filterByRiskLevel(
            @Param("clientId") Long clientId,
            @Param("riskLevel") String riskLevel,
            @Param("limit") int limit);
}
