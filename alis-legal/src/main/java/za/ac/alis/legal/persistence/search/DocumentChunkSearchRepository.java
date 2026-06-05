package za.ac.alis.legal.persistence.search;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import za.ac.alis.core.persistence.DocumentChunk;

public interface DocumentChunkSearchRepository extends Repository<DocumentChunk, Long> {

    @Query(value = """
            WITH search_query AS (
                SELECT plainto_tsquery('english', :query) AS value
            )
            SELECT
                dc.chunk_id AS "chunkId",
                d.document_id AS "documentId",
                d.title AS "documentTitle",
                dc.chunk_index AS "chunkIndex",
                dc.chunk_text AS "chunkText",
                d.client_id AS "clientId",
                CAST(ts_rank(dc.search_vector, search_query.value) AS double precision) AS "rank"
            FROM document_chunk dc
            JOIN document d ON d.document_id = dc.document_id
            CROSS JOIN search_query
            WHERE
                dc.search_vector @@ search_query.value
                AND (CAST(:clientId AS bigint) IS NULL
                    OR d.client_id = CAST(:clientId AS bigint))
                AND (CAST(:documentId AS bigint) IS NULL
                    OR d.document_id = CAST(:documentId AS bigint))
            ORDER BY "rank" DESC, d.uploaded_at DESC, dc.chunk_index ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<DocumentChunkSearchProjection> searchChunks(
            @Param("query") String query,
            @Param("clientId") Long clientId,
            @Param("documentId") Long documentId,
            @Param("limit") int limit);
}
