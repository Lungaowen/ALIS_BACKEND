package za.ac.alis.legal.port;

import org.springframework.stereotype.Component;

import za.ac.alis.core.enums.ActionType;
import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.port.AuditLogPort;
import za.ac.alis.legal.persistence.AuditLogRepository;
import za.ac.alis.legal.service.AuditLogService;

@Component
public class AuditLogPortImpl implements AuditLogPort {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    public AuditLogPortImpl(AuditLogService auditLogService, AuditLogRepository auditLogRepository) {
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logClientAction(Client client, ActionType actionType, String description, String ipAddress) {
        auditLogService.logClientAction(client, actionType, description, ipAddress);
    }

    @Override
    public void deleteByClientId(Long clientId) {
        auditLogRepository.deleteByClientClientId(clientId);
    }
}
