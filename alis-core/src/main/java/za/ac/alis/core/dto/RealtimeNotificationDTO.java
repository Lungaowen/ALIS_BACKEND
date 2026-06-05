package za.ac.alis.core.dto;

import java.time.LocalDateTime;

import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.SummaryReport;

public class RealtimeNotificationDTO {

    public static final String DOCUMENT_UPLOADED = "DOCUMENT_UPLOADED";
    public static final String DOCUMENT_INDEXED = "DOCUMENT_INDEXED";
    public static final String ANALYSIS_STARTED = "ANALYSIS_STARTED";
    public static final String REPORT_READY = "REPORT_READY";
    public static final String ANALYSIS_FAILED = "ANALYSIS_FAILED";

    private String type;
    private String message;
    private Long documentId;
    private String documentTitle;
    private String documentStatus;
    private Long clientId;
    private String clientName;
    private String clientRole;
    private Long reportId;
    private String riskLevel;
    private String analysisStatus;
    private Long actorId;
    private String actorRole;
    private String downloadUrl;
    private LocalDateTime createdAt;

    public static RealtimeNotificationDTO documentUploaded(Document document) {
        RealtimeNotificationDTO dto = fromDocument(document);
        dto.type = DOCUMENT_UPLOADED;
        dto.message = "New document uploaded by user #" + dto.clientId;
        return dto;
    }

    public static RealtimeNotificationDTO analysisStarted(
            Document document,
            Long actorId,
            String actorRole) {
        RealtimeNotificationDTO dto = fromDocument(document);
        dto.type = ANALYSIS_STARTED;
        dto.actorId = actorId;
        dto.actorRole = actorRole;
        dto.message = "Compliance analysis started for document #" + dto.documentId;
        return dto;
    }

    public static RealtimeNotificationDTO documentIndexed(Document document) {
        RealtimeNotificationDTO dto = fromDocument(document);
        dto.type = DOCUMENT_INDEXED;
        dto.message = "Document #" + dto.documentId + " is ready for legal search";
        return dto;
    }

    public static RealtimeNotificationDTO reportReady(SummaryReport report) {
        RealtimeNotificationDTO dto = fromDocument(report.getDocument());
        dto.type = REPORT_READY;
        dto.reportId = report.getReportId();
        dto.riskLevel = report.getRiskLevel() != null ? report.getRiskLevel().name() : null;
        dto.analysisStatus = report.getAnalysisStatus() != null
                ? report.getAnalysisStatus().name() : null;
        dto.downloadUrl = "/api/client/reports/" + report.getReportId() + "/download";
        dto.message = "Compliance report ready for document #" + dto.documentId;
        return dto;
    }

    public static RealtimeNotificationDTO analysisFailed(Document document, String reason) {
        RealtimeNotificationDTO dto = fromDocument(document);
        dto.type = ANALYSIS_FAILED;
        dto.analysisStatus = "FAILED";
        dto.message = reason == null || reason.isBlank()
                ? "Compliance analysis failed for document #" + dto.documentId
                : reason;
        return dto;
    }

    private static RealtimeNotificationDTO fromDocument(Document document) {
        RealtimeNotificationDTO dto = new RealtimeNotificationDTO();
        dto.createdAt = LocalDateTime.now();
        if (document != null) {
            dto.documentId = document.getDocumentId();
            dto.documentTitle = document.getTitle();
            dto.documentStatus = document.getStatus() != null
                    ? document.getStatus().name() : null;
            if (document.getClient() != null) {
                dto.clientId = document.getClient().getClientId();
                dto.clientName = document.getClient().getFullName();
                dto.clientRole = document.getClient().getRole() != null
                        ? document.getClient().getRole().name() : null;
            }
        }
        return dto;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientRole() {
        return clientRole;
    }

    public void setClientRole(String clientRole) {
        this.clientRole = clientRole;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
