package za.ac.alis.legal.persistence.search;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import za.ac.alis.core.persistence.Clause;

public interface ClauseSearchRepository extends Repository<Clause, Long> {

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            ),
            ranked_clauses AS (
                SELECT
                    c.clause_id AS "clauseId",
                    c.clause_text AS "clauseText",
                    CAST(c.risk_level AS text) AS "riskLevel",
                    c.risk_reason AS "riskReason",
                    c.page_number AS "pageNumber",
                    d.document_id AS "documentId",
                    d.title AS "documentTitle",
                    d.client_id AS "clientId",
                    d.uploaded_at AS "documentUploadedAt",
                    CAST(ts_rank(c.search_vector, search_query.value) AS double precision) AS "rank",
                    ROW_NUMBER() OVER (
                        PARTITION BY
                            d.document_id,
                            lower(btrim(regexp_replace(c.clause_text, '[[:space:]]+', ' ', 'g')))
                        ORDER BY ts_rank(c.search_vector, search_query.value) DESC, c.clause_id DESC
                    ) AS "dedupeRank"
                FROM clause c
                JOIN document d ON d.document_id = c.document_id
                CROSS JOIN search_query
                WHERE
                    c.search_vector @@ search_query.value
                    AND (CAST(:clientId AS bigint) IS NULL
                        OR d.client_id = CAST(:clientId AS bigint))
            )
            SELECT
                "clauseId",
                "clauseText",
                "riskLevel",
                "riskReason",
                "pageNumber",
                "documentId",
                "documentTitle",
                "clientId",
                "rank"
            FROM ranked_clauses
            WHERE "dedupeRank" = 1
            ORDER BY "rank" DESC, "documentUploadedAt" DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ClauseSearchProjection> searchClauses(
            @Param("query") String query,
            @Param("clientId") Long clientId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            )
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT
                    d.document_id,
                    lower(btrim(regexp_replace(c.clause_text, '[[:space:]]+', ' ', 'g'))) AS normalized_clause_text
                FROM clause c
                JOIN document d ON d.document_id = c.document_id
                CROSS JOIN search_query
                WHERE
                    c.search_vector @@ search_query.value
                    AND (CAST(:clientId AS bigint) IS NULL
                        OR d.client_id = CAST(:clientId AS bigint))
            ) deduped_clauses
            """, nativeQuery = true)
    long countClauseSearch(
            @Param("query") String query,
            @Param("clientId") Long clientId);
}
