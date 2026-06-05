package za.ac.alis.user.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.enums.Role;
import za.ac.alis.core.projections.ClientActivityProjection;
import za.ac.alis.core.projections.ClientDetailProjection;
import za.ac.alis.core.projections.DocumentCountProjection;
import za.ac.alis.core.projections.RoleStatProjection;
import za.ac.alis.core.projections.TopUploaderProjection;
import za.ac.alis.core.projections.MonthlyCountProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static za.ac.alis.core.queries.AdminDashboardQueries.FIND_CLIENT_ACTIVITIES;
import static za.ac.alis.core.queries.AdminDashboardQueries.ROLE_DISTRIBUTION;
import static za.ac.alis.core.queries.ClientQueries.*;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // ─────────────────────────────────────────────
    // ENTITY LOOKUPS (auth / update / delete only)
    // ─────────────────────────────────────────────

    Optional<Client> findByEmail(String email);
    Optional<Client> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    long countByRole(Role role);

    // ─────────────────────────────────────────────
    // LIST & SEARCH → ClientDetailProjection
    // ─────────────────────────────────────────────

    @Query(FIND_ALL_CLIENTS)
    Page<ClientDetailProjection> findAllClients(Pageable pageable);

    @Query(SEARCH_CLIENTS)
    Page<ClientDetailProjection> searchClients(
            @Param("query") String query, Pageable pageable);

    @Query(FILTER_CLIENTS)
    Page<ClientDetailProjection> findByFilters(
            @Param("role") Role role,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            Pageable pageable);

    @Query(FIND_BY_ROLE)
    Page<ClientDetailProjection> findClientsByRole(
            @Param("role") Role role, Pageable pageable);

    @Query(FIND_BY_DATE_RANGE)
    Page<ClientDetailProjection> findClientsByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            Pageable pageable);

    @Query(FIND_RECENTLY_REGISTERED)
    List<ClientDetailProjection> findRecentlyRegistered(
            @Param("since") LocalDateTime since);

    @Query(FIND_CLIENTS_WITH_NO_DOCUMENTS)
    List<ClientDetailProjection> findClientsWithNoDocuments();

    // ─────────────────────────────────────────────
    // DOCUMENT COUNTS
    // ─────────────────────────────────────────────

    @Query(COUNT_DOCUMENTS_BY_CLIENT_ID)
    Long countDocumentsByClientId(@Param("clientId") Long clientId);

    @Query(COUNT_DOCUMENTS_PER_CLIENT)
    List<DocumentCountProjection> countDocumentsPerClient();

    // ─────────────────────────────────────────────
    // REPORTS
    // ─────────────────────────────────────────────

    @Query(COUNT_BY_ROLE)
    List<RoleStatProjection> countClientsByRole();

    @Query(COUNT_REGISTRATIONS_BY_MONTH)
    List<MonthlyCountProjection> countRegistrationsByMonth(
            @Param("since") LocalDateTime since);

    @Query(FIND_TOP_UPLOADERS)
    Page<TopUploaderProjection> findTopUploaders(Pageable pageable);

    // ─────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────

    @Query(FIND_CLIENT_ACTIVITIES)
    List<ClientActivityProjection> findClientActivities(Pageable pageable);

    @Query(ROLE_DISTRIBUTION)
    List<RoleStatProjection> findRoleDistribution();
}
