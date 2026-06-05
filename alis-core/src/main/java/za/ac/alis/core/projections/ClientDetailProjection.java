package za.ac.alis.core.projections;

import za.ac.alis.core.enums.Role;
import java.time.LocalDateTime;

/**
 * Interface projection — Spring Data maps query columns to getter names automatically.
 * No constructor. No SemanticException. No inner class issues.
 *
 * Query must alias columns to match getter names (minus "get", lowercased first letter):
 *   c.clientId   AS clientId
 *   c.fullName   AS fullName  ... etc.
 */
public interface ClientDetailProjection {
    Long          getClientId();
    String        getFullName();
    String        getEmail();
    String        getUsername();
    Role          getRole();
    LocalDateTime getCreatedAt();
}
