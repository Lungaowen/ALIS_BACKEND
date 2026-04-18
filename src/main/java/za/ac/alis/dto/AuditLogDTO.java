package za.ac.alis.dto;

import za.ac.alis.enums.ActionType;
import java.time.LocalDateTime;

public class AuditLogDTO {

    private final Long logId;
    private final ActionType actionType;
    private final String description;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    private final Long clientId;
    private final Long adminId;
    private final Long documentId;

    public AuditLogDTO(Long logId, ActionType actionType, String description,
                       String ipAddress, LocalDateTime createdAt,
                       Long clientId, Long adminId, Long documentId) {
        this.logId = logId;
        this.actionType = actionType;
        this.description = description;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.clientId = clientId;
        this.adminId = adminId;
        this.documentId = documentId;
    }

    // Getters only
    public Long getLogId() { return logId; }
    public ActionType getActionType() { return actionType; }
    public String getDescription() { return description; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getClientId() { return clientId; }
    public Long getAdminId() { return adminId; }
    public Long getDocumentId() { return documentId; }
}