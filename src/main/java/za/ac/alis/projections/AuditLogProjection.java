package za.ac.alis.projections;

import za.ac.alis.enums.ActionType;
import java.time.LocalDateTime;

/**
 * Interface projection for AuditLog queries.
 * No entity loaded — no LazyInitializationException possible.
 */
public interface AuditLogProjection {
    Long          getLogId();
    ActionType    getActionType();
    String        getDescription();
    String        getIpAddress();
    LocalDateTime getCreatedAt();
    Long          getClientId();
    Long          getAdminId();
    Long          getDocumentId();
}
