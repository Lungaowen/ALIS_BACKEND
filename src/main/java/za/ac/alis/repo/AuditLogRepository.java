package za.ac.alis.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.alis.entities.AuditLog;
import za.ac.alis.projections.AuditLogProjection;

import java.util.List;

import static za.ac.alis.queries.AuditLogQueries.*;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(FIND_ALL_LOGS)
    List<AuditLogProjection> findAllLogs();

    @Query(FIND_RECENT_LOGS)
    List<AuditLogProjection> findRecentLogs(Pageable pageable);

    @Query(FIND_LOGS_BY_CLIENT_ID)
    List<AuditLogProjection> findLogsByClientId(@Param("clientId") Long clientId);
}
