// src/main/java/za/ac/alis/entities/SummaryReport.java
package za.ac.alis.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import za.ac.alis.enums.AnalysisStatus;
import za.ac.alis.enums.RiskLevel;
import za.ac.alis.enums.DocumentStat;
import java.math.BigDecimal;

@Entity
@Table(name = "summary_report")
public class SummaryReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private LawRul lawRule;

    private BigDecimal similarityScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(columnDefinition = "TEXT")
    private String aiExplanation;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus;

    private LocalDateTime generatedAt;

    private String modelVersion;

    /**
     * FULL RICH COMPLIANCE REPORT as JSONB
     */
    @Column(name = "report_summary_json", columnDefinition = "jsonb")
    private String reportSummaryJson;

    // ====================== TRADITIONAL GETTERS & SETTERS ======================

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LawRul getLawRule() {
        return lawRule;
    }

    public void setLawRule(LawRul lawRule) {
        this.lawRule = lawRule;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(BigDecimal similarityScore) {
        this.similarityScore = similarityScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getReportSummaryJson() {
        return reportSummaryJson;
    }

    public void setReportSummaryJson(String reportSummaryJson) {
        this.reportSummaryJson = reportSummaryJson;
    }

    public void setReportSummaryJson(Object jsonObject) {
        if (jsonObject != null) {
            this.reportSummaryJson = jsonObject.toString();
        } else {
            this.reportSummaryJson = null;
        }
    }
}