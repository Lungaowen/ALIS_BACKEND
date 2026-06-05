package za.ac.alis.legal.persistence.search;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import za.ac.alis.core.persistence.Document;

public interface DocumentSearchRepository extends Repository<Document, Long> {

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            ),
            ranked_documents AS (
                SELECT
                    d.document_id AS "documentId",
                    d.title AS "title",
                    CAST(d.status AS text) AS "status",
                    d.uploaded_at AS "uploadedAt",
                    d.client_id AS "clientId",
                    CAST(ts_rank(
                        setweight(to_tsvector(
                            'english',
                            regexp_replace(coalesce(d.title, ''), '[^[:alnum:]]+', ' ', 'g')
                        ), 'A') ||
                            setweight(coalesce(d.search_vector, CAST('' AS tsvector)), 'B') ||
                            setweight(coalesce(dc.search_vector, CAST('' AS tsvector)), 'D'),
                        search_query.value
                    ) AS double precision) AS "rank",
                    ROW_NUMBER() OVER (
                        PARTITION BY d.document_id
                        ORDER BY ts_rank(
                            setweight(to_tsvector(
                                'english',
                                regexp_replace(coalesce(d.title, ''), '[^[:alnum:]]+', ' ', 'g')
                            ), 'A') ||
                                setweight(coalesce(d.search_vector, CAST('' AS tsvector)), 'B') ||
                                setweight(coalesce(dc.search_vector, CAST('' AS tsvector)), 'D'),
                            search_query.value
                        ) DESC, d.uploaded_at DESC
                    ) AS "dedupeRank"
                FROM document d
                LEFT JOIN document_content dc ON dc.document_id = d.document_id
                CROSS JOIN search_query
                WHERE
                    (setweight(to_tsvector(
                        'english',
                        regexp_replace(coalesce(d.title, ''), '[^[:alnum:]]+', ' ', 'g')
                    ), 'A') ||
                        setweight(coalesce(d.search_vector, CAST('' AS tsvector)), 'B') ||
                        setweight(coalesce(dc.search_vector, CAST('' AS tsvector)), 'D')) @@ search_query.value
                    AND (CAST(:clientId AS bigint) IS NULL
                        OR d.client_id = CAST(:clientId AS bigint))
            )
            SELECT
                "documentId",
                "title",
                "status",
                "uploadedAt",
                "clientId",
                "rank"
            FROM ranked_documents
            WHERE "dedupeRank" = 1
            ORDER BY "rank" DESC, "uploadedAt" DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<DocumentSearchProjection> searchDocuments(
            @Param("query") String query,
            @Param("clientId") Long clientId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            )
            SELECT COUNT(DISTINCT d.document_id)
            FROM document d
            LEFT JOIN document_content dc ON dc.document_id = d.document_id
            CROSS JOIN search_query
            WHERE
                (setweight(to_tsvector(
                    'english',
                    regexp_replace(coalesce(d.title, ''), '[^[:alnum:]]+', ' ', 'g')
                ), 'A') ||
                    setweight(coalesce(d.search_vector, CAST('' AS tsvector)), 'B') ||
                    setweight(coalesce(dc.search_vector, CAST('' AS tsvector)), 'D')) @@ search_query.value
                AND (CAST(:clientId AS bigint) IS NULL
                    OR d.client_id = CAST(:clientId AS bigint))
            """, nativeQuery = true)
    long countDocumentSearch(
            @Param("query") String query,
            @Param("clientId") Long clientId);
}
