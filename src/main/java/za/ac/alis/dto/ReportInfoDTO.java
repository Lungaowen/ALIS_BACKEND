// src/main/java/za/ac/alis/dto/ReportInfoDTO.java
package za.ac.alis.dto;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.projections.ClientDetailProjection; 
// if needed
import za.ac.alis.projections.ReportInfoProjection;
import za.ac.alis.enums.AnalysisStatus;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportInfoDTO {

    private Long reportId;
    private Long documentId;
    private Long clientId;
    private String documentTitle;
    private RiskLevel riskLevel;
    private AnalysisStatus analysisStatus;
    private String aiRecommendation;
    private String aiExplanation;
    private LocalDateTime generatedAt;
    private String modelVersion;
    private String reportSummaryJson;
    private BigDecimal similarityScore;

    // ====================== STATIC FACTORY METHODS ======================

    public static ReportInfoDTO fromEntity(SummaryReport entity) {
        if (entity == null) return null;

        ReportInfoDTO dto = new ReportInfoDTO();
        dto.setReportId(entity.getReportId());
        dto.setDocumentId(entity.getDocument() != null ? entity.getDocument().getDocumentId() : null);
        dto.setClientId(entity.getClient() != null ? entity.getClient().getClientId() : null);
        dto.setDocumentTitle(entity.getDocument() != null ? entity.getDocument().getTitle() : null);
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setAnalysisStatus(entity.getAnalysisStatus());
        dto.setAiRecommendation(entity.getAiRecommendation());
        dto.setAiExplanation(entity.getAiExplanation());
        dto.setGeneratedAt(entity.getGeneratedAt());
        dto.setModelVersion(entity.getModelVersion());
        dto.setReportSummaryJson(entity.getReportSummaryJson());
        dto.setSimilarityScore(entity.getSimilarityScore());
        return dto;
    }

    public static ReportInfoDTO fromProjection(Object projection) {
        // Implement if needed for AdminDashboardService
        // For now, return empty or extend as required
        return new ReportInfoDTO();
    }

    // ====================== TRADITIONAL GETTERS & SETTERS ======================

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public AnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(AnalysisStatus analysisStatus) { this.analysisStatus = analysisStatus; }

    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getReportSummaryJson() { return reportSummaryJson; }
    public void setReportSummaryJson(String reportSummaryJson) { this.reportSummaryJson = reportSummaryJson; }

    public BigDecimal getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(BigDecimal similarityScore) { this.similarityScore = similarityScore; }
}