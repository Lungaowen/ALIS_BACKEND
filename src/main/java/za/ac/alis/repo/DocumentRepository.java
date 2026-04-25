package za.ac.alis.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.alis.entities.Document;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.projections.DocumentInfoProjection;
import za.ac.alis.projections.MonthlyUploadProjection;

import java.time.LocalDateTime;
import java.util.List;

import static za.ac.alis.queries.AdminDashboardQueries.*;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    long countByStatus(DocumentStat status);

    @Query(COUNT_ACTIVE_CLIENTS)
    long countActiveClients();

    @Query(FIND_RECENT_DOCUMENTS)
    List<DocumentInfoProjection> findRecentDocuments(Pageable pageable);

    @Query(MONTHLY_UPLOADS)
    List<MonthlyUploadProjection> findMonthlyUploads(@Param("since") LocalDateTime since);

    // Additional methods needed by services
    List<Document> findByClient_ClientId(Long clientId);

    @Query("SELECT d FROM Document d JOIN FETCH d.client WHERE d.client.clientId = :clientId")
    List<Document> findDocumentsByClientWithClient(@Param("clientId") Long clientId);

    @Query("SELECT COUNT(DISTINCT d.client.clientId) FROM Document d")
    long countDistinctClientIds();

    @Query("""
            SELECT d.documentId      AS documentId,
                   d.title           AS title,
                   d.status          AS status,
                   d.ingestionSource AS ingestionSource,
                   d.uploadedAt      AS uploadedAt,
                   d.filePath        AS filePath,
                   d.fileUrl         AS fileUrl,
                   c.clientId        AS clientId,
                   c.fullName        AS clientName
            FROM Document d
            JOIN d.client c
            WHERE d.client.clientId = :clientId
            ORDER BY d.uploadedAt DESC
            """)
    List<DocumentInfoProjection> findRecentDocumentsByClientId(@Param("clientId") Long clientId);
}
