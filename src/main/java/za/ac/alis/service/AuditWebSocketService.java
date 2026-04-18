package za.ac.alis.service;

import za.ac.alis.entities.AuditLog;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public AuditWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastAuditLog(AuditLog auditLog) {
        messagingTemplate.convertAndSend("/topic/audit-logs", auditLog);
    }

    public void sendToUser(String username, AuditLog auditLog) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/audit-logs",
                auditLog
        );
    }
}