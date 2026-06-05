package za.ac.alis.legal.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import za.ac.alis.core.dto.RealtimeNotificationDTO;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.SummaryReport;

@Service
public class NotificationWebSocketService {

    public static final String LEGAL_PRACTITIONER_NOTIFICATIONS =
            "/topic/legal-practitioners/notifications";
    public static final String LEGAL_PRACTITIONER_DOCUMENTS =
            "/topic/legal-practitioners/documents";
    public static final String LEGAL_PRACTITIONER_REPORTS =
            "/topic/legal-practitioners/reports";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyDocumentUploaded(Document document) {
        RealtimeNotificationDTO notification =
                RealtimeNotificationDTO.documentUploaded(document);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_NOTIFICATIONS, notification);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_DOCUMENTS, notification);
    }

    public void notifyDocumentIndexed(Document document) {
        RealtimeNotificationDTO notification =
                RealtimeNotificationDTO.documentIndexed(document);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_NOTIFICATIONS, notification);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_DOCUMENTS, notification);
    }

    public void notifyAnalysisStarted(Document document, Long actorId, String actorRole) {
        RealtimeNotificationDTO notification =
                RealtimeNotificationDTO.analysisStarted(document, actorId, actorRole);
        messagingTemplate.convertAndSend(clientNotificationsTopic(notification.getClientId()), notification);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_NOTIFICATIONS, notification);
    }

    public void notifyReportReady(SummaryReport report) {
        RealtimeNotificationDTO notification = RealtimeNotificationDTO.reportReady(report);
        messagingTemplate.convertAndSend(clientNotificationsTopic(notification.getClientId()), notification);
        messagingTemplate.convertAndSend(clientReportsTopic(notification.getClientId()), notification);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_REPORTS, notification);
    }

    public void notifyAnalysisFailed(Document document, String reason) {
        RealtimeNotificationDTO notification =
                RealtimeNotificationDTO.analysisFailed(document, reason);
        messagingTemplate.convertAndSend(clientNotificationsTopic(notification.getClientId()), notification);
        messagingTemplate.convertAndSend(LEGAL_PRACTITIONER_REPORTS, notification);
    }

    private String clientNotificationsTopic(Long clientId) {
        return "/topic/clients/" + clientId + "/notifications";
    }

    private String clientReportsTopic(Long clientId) {
        return "/topic/clients/" + clientId + "/reports";
    }
}
