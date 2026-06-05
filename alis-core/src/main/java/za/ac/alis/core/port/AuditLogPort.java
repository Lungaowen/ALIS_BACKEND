package za.ac.alis.core.port;

import za.ac.alis.core.enums.ActionType;
import za.ac.alis.core.persistence.Client;

public interface AuditLogPort {

    void logClientAction(Client client, ActionType actionType, String description, String ipAddress);

    void deleteByClientId(Long clientId);
}
