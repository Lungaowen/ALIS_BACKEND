package za.ac.alis.core.dto;

import java.time.Instant;

public class ReportDownloadUrlDTO {

    private Long reportId;
    private String reportUrl;
    private String signedUrl;
    private Instant expiresAt;

    public ReportDownloadUrlDTO() {
    }

    public ReportDownloadUrlDTO(Long reportId, String reportUrl, String signedUrl, Instant expiresAt) {
        this.reportId = reportId;
        this.reportUrl = reportUrl;
        this.signedUrl = signedUrl;
        this.expiresAt = expiresAt;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getSignedUrl() {
        return signedUrl;
    }

    public void setSignedUrl(String signedUrl) {
        this.signedUrl = signedUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
