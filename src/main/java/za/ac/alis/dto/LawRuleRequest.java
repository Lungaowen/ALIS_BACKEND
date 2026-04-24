package za.ac.alis.dto;

import za.ac.alis.enums.RiskLevel;

public class LawRuleRequest {
    private Long actId;
    private String keyword;
    private String requirements;
    private RiskLevel riskLevel;
    private String suggestion;

    // getters and setters
    public Long getActId() { return actId; }
    public void setActId(Long actId) { this.actId = actId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}