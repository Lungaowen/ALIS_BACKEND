package za.ac.alis.core.dto;

import za.ac.alis.core.enums.ActionType;
import za.ac.alis.core.projections.AuditLogProjection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Frontend-safe AuditLog response.
 * Built from AuditLogProjection — no entity, no lazy loading.
 */
public class AuditLogDTO {

    private Long       logId;
    private String     actionType;   // enum.name() — string for frontend
    private String     description;
    private String     ipAddress;
    private String     createdAt;    // ISO string
    private Long       clientId;
    private Long       adminId;
    private Long       documentId;

    public static AuditLogDTO from(AuditLogProjection p) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.logId       = p.getLogId();
        dto.actionType  = p.getActionType() != null ? p.getActionType().name() : null;
        dto.description = p.getDescription();
        dto.ipAddress   = p.getIpAddress();
        dto.createdAt   = p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        dto.clientId    = p.getClientId();
        dto.adminId     = p.getAdminId();
        dto.documentId  = p.getDocumentId();
        return dto;
    }

    public AuditLogDTO() {}

    public Long   getLogId()       { return logId; }
    public String getActionType()  { return actionType; }
    public String getDescription() { return description; }
    public String getIpAddress()   { return ipAddress; }
    public String getCreatedAt()   { return createdAt; }
    public Long   getClientId()    { return clientId; }
    public Long   getAdminId()     { return adminId; }
    public Long   getDocumentId()  { return documentId; }
}
