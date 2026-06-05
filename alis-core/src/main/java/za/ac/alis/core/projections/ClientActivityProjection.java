package za.ac.alis.core.projections;

import za.ac.alis.core.enums.Role;
import java.time.LocalDateTime;

/**
 * Interface projection for client activity rows in the dashboard.
 * documentCount comes from COUNT(d.documentId) aliased as documentCount.
 */
public interface ClientActivityProjection {
    Long          getClientId();
    String        getFullName();
    String        getEmail();
    Role          getRole();
    LocalDateTime getCreatedAt();
    Long          getDocumentCount();
}
