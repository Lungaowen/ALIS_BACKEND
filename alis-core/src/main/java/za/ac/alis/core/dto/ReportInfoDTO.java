package za.ac.alis.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.core.enums.AnalysisStatus;
import za.ac.alis.core.enums.RiskLevel;
import za.ac.alis.core.projections.ReportInfoProjection;

public class ReportInfoDTO {

    private Long           reportId;
    private Long           documentId;
    private String         documentTitle;
    private Long           clientId;
    private String         clientName;
    private RiskLevel      riskLevel;
    private AnalysisStatus analysisStatus;
    private BigDecimal     similarityScore;
    private String         aiRecommendation;
    private String         aiExplanation;
    private LocalDateTime  generatedAt;
    private String         modelVersion;
    private String         reportUrl;
    // The primary law rule that was matched
    private Long           lawRuleId;
    private String         lawRuleKeyword;
    private String         actName;

    public ReportInfoDTO() {}

    // ── From interface projection (dashboard queries) ──────────────────────────
    public static ReportInfoDTO fromProjection(ReportInfoProjection p) {
        ReportInfoDTO dto = new ReportInfoDTO();
        dto.reportId         = p.getReportId();
        dto.documentId       = p.getDocumentId();
        dto.documentTitle    = p.getDocumentTitle();
        dto.riskLevel        = p.getRiskLevel();
        dto.analysisStatus   = p.getAnalysisStatus();
        dto.aiRecommendation = p.getAiRecommendation();
        dto.generatedAt      = p.getGeneratedAt();
        dto.modelVersion     = p.getModelVersion();
        return dto;
    }

    // ── From entity (ReportController / ComplianceController) ─────────────────
    public static ReportInfoDTO fromEntity(SummaryReport r) {
        ReportInfoDTO dto = new ReportInfoDTO();
        dto.reportId         = r.getReportId();
        dto.riskLevel        = r.getRiskLevel();
        dto.analysisStatus   = r.getAnalysisStatus();
        dto.similarityScore  = r.getSimilarityScore();
        dto.aiRecommendation = r.getAiRecommendation();
        dto.aiExplanation    = r.getAiExplanation();
        dto.generatedAt      = r.getGeneratedAt();
        dto.modelVersion     = r.getModelVersion();
        dto.reportUrl        = r.getReportUrl();

        if (r.getDocument() != null) {
            dto.documentId    = r.getDocument().getDocumentId();
            dto.documentTitle = r.getDocument().getTitle();
        }
        if (r.getClient() != null) {
            dto.clientId   = r.getClient().getClientId();
            dto.clientName = r.getClient().getFullName();
        }
        // lawRule is nullable (Groq may not always match a rule)
        if (r.getLawRule() != null) {
            dto.lawRuleId      = r.getLawRule().getRuleId();
            dto.lawRuleKeyword = r.getLawRule().getKeyword();
            if (r.getLawRule().getAct() != null) {
                dto.actName = r.getLawRule().getAct().getActName();
            }
        }
        return dto;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public Long           getReportId()         { return reportId; }
    public Long           getDocumentId()       { return documentId; }
    public String         getDocumentTitle()    { return documentTitle; }
    public Long           getClientId()         { return clientId; }
    public String         getClientName()       { return clientName; }
    public RiskLevel      getRiskLevel()        { return riskLevel; }
    public AnalysisStatus getAnalysisStatus()   { return analysisStatus; }
    public BigDecimal     getSimilarityScore()  { return similarityScore; }
    public String         getAiRecommendation() { return aiRecommendation; }
    public String         getAiExplanation()    { return aiExplanation; }
    public LocalDateTime  getGeneratedAt()      { return generatedAt; }
    public String         getModelVersion()     { return modelVersion; }
    public String         getReportUrl()        { return reportUrl; }
    public Long           getLawRuleId()        { return lawRuleId; }
    public String         getLawRuleKeyword()   { return lawRuleKeyword; }
    public String         getActName()          { return actName; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setReportId(Long v)           { this.reportId = v; }
    public void setDocumentId(Long v)         { this.documentId = v; }
    public void setDocumentTitle(String v)    { this.documentTitle = v; }
    public void setClientId(Long v)           { this.clientId = v; }
    public void setClientName(String v)       { this.clientName = v; }
    public void setRiskLevel(RiskLevel v)     { this.riskLevel = v; }
    public void setAnalysisStatus(AnalysisStatus v) { this.analysisStatus = v; }
    public void setSimilarityScore(BigDecimal v)    { this.similarityScore = v; }
    public void setAiRecommendation(String v) { this.aiRecommendation = v; }
    public void setAiExplanation(String v)    { this.aiExplanation = v; }
    public void setGeneratedAt(LocalDateTime v) { this.generatedAt = v; }
    public void setModelVersion(String v)     { this.modelVersion = v; }
    public void setReportUrl(String v)        { this.reportUrl = v; }
    public void setLawRuleId(Long v)          { this.lawRuleId = v; }
    public void setLawRuleKeyword(String v)   { this.lawRuleKeyword = v; }
    public void setActName(String v)          { this.actName = v; }
}
