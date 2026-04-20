package za.ac.alis.dto;

/**
 * Replaces raw Document entity returned by DocumentController.
 * Only exposes fields the frontend needs — no lazy collections.
 */
public class DocumentResponseDTO {

    private Long   documentId;
    private String title;
    private String status;
    private String ingestionSource;
    private String uploadedAt;
    private String filePath;
    private String fileUrl;
    private Long   clientId;

    public DocumentResponseDTO() {}

    public Long   getDocumentId()      { return documentId; }
    public void   setDocumentId(Long v){ this.documentId = v; }
    public String getTitle()           { return title; }
    public void   setTitle(String v)   { this.title = v; }
    public String getStatus()          { return status; }
    public void   setStatus(String v)  { this.status = v; }
    public String getIngestionSource() { return ingestionSource; }
    public void   setIngestionSource(String v) { this.ingestionSource = v; }
    public String getUploadedAt()      { return uploadedAt; }
    public void   setUploadedAt(String v){ this.uploadedAt = v; }
    public String getFilePath()        { return filePath; }
    public void   setFilePath(String v){ this.filePath = v; }
    public String getFileUrl()         { return fileUrl; }
    public void   setFileUrl(String v) { this.fileUrl = v; }
    public Long   getClientId()        { return clientId; }
    public void   setClientId(Long v)  { this.clientId = v; }
}
