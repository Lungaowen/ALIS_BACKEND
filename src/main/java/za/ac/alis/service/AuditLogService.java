package za.ac.alis.service;

import za.ac.alis.entities.AuditLog;
import za.ac.alis.entities.Admin;
import za.ac.alis.entities.Client;
import za.ac.alis.entities.Document;
import za.ac.alis.enums.ActionType;
import za.ac.alis.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditWebSocketService webSocketService;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           AuditWebSocketService webSocketService) {
        this.auditLogRepository = auditLogRepository;
        this.webSocketService = webSocketService;
    }

    public AuditLog logAction(Admin admin,
                              Client client,
                              Document document,
                              ActionType actionType,
                              String description,
                              String ipAddress) {

        AuditLog log = new AuditLog();
        log.setAdmin(admin);
        log.setClient(client);
        log.setDocument(document);
        log.setActionType(actionType);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());

        AuditLog savedLog = auditLogRepository.save(log);

        // real-time push
        webSocketService.broadcastAuditLog(savedLog);

        return savedLog;
    }

    public AuditLog logClientAction(Client client, ActionType actionType, String description, String ip) {
        return logAction(null, client, null, actionType, description, ip);
    }

    public AuditLog logAdminAction(Admin admin, ActionType actionType, String description, String ip) {
        return logAction(admin, null, null, actionType, description, ip);
    }

    public AuditLog logDocumentAction(Client client, Document document, ActionType actionType, String description, String ip) {
        return logAction(null, client, document, actionType, description, ip);
    }
}