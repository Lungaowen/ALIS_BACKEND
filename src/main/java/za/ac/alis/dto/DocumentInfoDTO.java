package za.ac.alis.dto;

import za.ac.alis.projections.DocumentInfoProjection;
import java.time.format.DateTimeFormatter;

/**
 * Response DTO assembled from DocumentInfoProjection in the service layer.
 * All fields are Strings — no enums, no LocalDateTime leaking to frontend.
 * No broken setters, no UnsupportedOperationException.
 */
public class DocumentInfoDTO {

    private Long   documentId;
    private String title;
    private String status;          // enum.name() — String
    private String ingestionSource; // enum.name() — String
    private String uploadedAt;      // ISO string
    private String filePath;
    private String fileUrl;
    private Long   clientId;
    private String clientName;

    // ── Built from projection in service ─────────────────────────────────────
    public static DocumentInfoDTO from(DocumentInfoProjection p) {
        DocumentInfoDTO dto = new DocumentInfoDTO();
        dto.documentId      = p.getDocumentId();
        dto.title           = p.getTitle();
        dto.status          = p.getStatus() != null ? p.getStatus().name() : null;
        dto.ingestionSource = p.getIngestionSource() != null ? p.getIngestionSource().name() : null;
        dto.uploadedAt      = p.getUploadedAt() != null
                ? p.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        dto.filePath        = p.getFilePath();
        dto.fileUrl         = p.getFileUrl();
        dto.clientId        = p.getClientId();
        dto.clientName      = p.getClientName();
        return dto;
    }

    public DocumentInfoDTO() {}

    public Long   getDocumentId()      { return documentId; }
    public String getTitle()           { return title; }
    public String getStatus()          { return status; }
    public String getIngestionSource() { return ingestionSource; }
    public String getUploadedAt()      { return uploadedAt; }
    public String getFilePath()        { return filePath; }
    public String getFileUrl()         { return fileUrl; }
    public Long   getClientId()        { return clientId; }
    public String getClientName()      { return clientName; }
}
