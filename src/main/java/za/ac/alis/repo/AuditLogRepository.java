package za.ac.alis.repo;

import za.ac.alis.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // BEST SOLUTION: JOIN FETCH to load relationships in one query
   @Query("""
SELECT a FROM AuditLog a
LEFT JOIN FETCH a.client
LEFT JOIN FETCH a.admin
LEFT JOIN FETCH a.document
ORDER BY a.createdAt DESC
""")
List<AuditLog> findAllLogs();

    // Top 20 recent logs with relationships
    @Query("""
        SELECT a FROM AuditLog a
        LEFT JOIN FETCH a.client
        LEFT JOIN FETCH a.admin
        LEFT JOIN FETCH a.document
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> getRecentLogs();

    // Logs by client
    @Query("""
        SELECT a FROM AuditLog a
        LEFT JOIN FETCH a.client
        LEFT JOIN FETCH a.admin
        LEFT JOIN FETCH a.document
        WHERE a.client.clientId = :clientId
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> getLogsByClientId(Long clientId);

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();

    public List<AuditLog> findByClient_ClientIdOrderByCreatedAtDesc(Long clientId);
}