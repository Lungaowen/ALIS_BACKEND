package za.ac.alis.core.dto;

/**
 * Document response sent to the frontend.
 * Includes riskLevel and reportId from the latest summary_report
 * so the frontend doesn't need a separate query.
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
    // From latest summary_report (may be null if not yet analyzed)
    private String riskLevel;
    private Long   reportId;
    private Double similarityScore;

    public DocumentResponseDTO() {}

    public Long   getDocumentId()        { return documentId; }
    public void   setDocumentId(Long v)  { this.documentId = v; }
    public String getTitle()             { return title; }
    public void   setTitle(String v)     { this.title = v; }
    public String getStatus()            { return status; }
    public void   setStatus(String v)    { this.status = v; }
    public String getIngestionSource()   { return ingestionSource; }
    public void   setIngestionSource(String v) { this.ingestionSource = v; }
    public String getUploadedAt()        { return uploadedAt; }
    public void   setUploadedAt(String v){ this.uploadedAt = v; }
    public String getFilePath()          { return filePath; }
    public void   setFilePath(String v)  { this.filePath = v; }
    public String getFileUrl()           { return fileUrl; }
    public void   setFileUrl(String v)   { this.fileUrl = v; }
    public Long   getClientId()          { return clientId; }
    public void   setClientId(Long v)    { this.clientId = v; }
    public String getRiskLevel()         { return riskLevel; }
    public void   setRiskLevel(String v) { this.riskLevel = v; }
    public Long   getReportId()          { return reportId; }
    public void   setReportId(Long v)    { this.reportId = v; }
    public Double getSimilarityScore()   { return similarityScore; }
    public void   setSimilarityScore(Double v) { this.similarityScore = v; }
}
