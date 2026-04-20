package za.ac.alis.dto;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.enums.AnalysisStatus;
import za.ac.alis.enums.RiskLevel;
import za.ac.alis.projections.ReportInfoProjection;

import java.time.LocalDateTime;

public class ReportInfoDTO {

    private Long reportId;
    private Long documentId;
    private String documentTitle;
    private RiskLevel riskLevel;
    private AnalysisStatus analysisStatus;
    private String aiRecommendation;
    private LocalDateTime generatedAt;
    private String modelVersion;

    // Default constructor
    public ReportInfoDTO() {}

    // Factory method for projection (used in dashboard services)
    public static ReportInfoDTO fromProjection(ReportInfoProjection p) {
        ReportInfoDTO dto = new ReportInfoDTO();
        dto.reportId = p.getReportId();
        dto.documentId = p.getDocumentId();
        dto.documentTitle = p.getDocumentTitle();
        dto.riskLevel = p.getRiskLevel();
        dto.analysisStatus = p.getAnalysisStatus();
        dto.aiRecommendation = p.getAiRecommendation();
        dto.generatedAt = p.getGeneratedAt();
        dto.modelVersion = p.getModelVersion();
        return dto;
    }

    // Factory method for entity (used in ReportController)
    public static ReportInfoDTO fromEntity(SummaryReport r) {
        ReportInfoDTO dto = new ReportInfoDTO();
        dto.reportId = r.getReportId();
        dto.documentId = r.getDocument() != null ? r.getDocument().getDocumentId() : null;
        dto.documentTitle = r.getDocument() != null ? r.getDocument().getTitle() : null;
        dto.riskLevel = r.getRiskLevel();
        dto.analysisStatus = r.getAnalysisStatus();
        dto.aiRecommendation = r.getAiRecommendation();
        dto.generatedAt = r.getGeneratedAt();
        dto.modelVersion = r.getModelVersion();
        return dto;
    }

    // Getters and Setters
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public AnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(AnalysisStatus analysisStatus) { this.analysisStatus = analysisStatus; }
    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
}