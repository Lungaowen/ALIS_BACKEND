package za.ac.alis.legal.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.core.persistence.AuditLog;
import za.ac.alis.core.projections.AuditLogProjection;

import java.util.List;

import static za.ac.alis.core.queries.AuditLogQueries.*;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(FIND_ALL_LOGS)
    List<AuditLogProjection> findAllLogs();

    @Query(FIND_RECENT_LOGS)
    List<AuditLogProjection> findRecentLogs(Pageable pageable);

    @Query(FIND_LOGS_BY_CLIENT_ID)
    List<AuditLogProjection> findLogsByClientId(@Param("clientId") Long clientId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.document.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.client.clientId = :clientId")
    void deleteByClientClientId(@Param("clientId") Long clientId);
}
