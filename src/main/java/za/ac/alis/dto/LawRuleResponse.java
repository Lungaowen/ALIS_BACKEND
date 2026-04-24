package za.ac.alis.dto;

import za.ac.alis.enums.RiskLevel;

public class LawRuleResponse {
    private Long ruleId;
    private Long actId;
    private String actName;
    private String keyword;
    private String requirements;
    private RiskLevel riskLevel;
    private String suggestion;
    private Boolean edited;

    public LawRuleResponse(Long ruleId, Long actId, String actName, String keyword,
                           String requirements, RiskLevel riskLevel, String suggestion, Boolean edited) {
        this.ruleId = ruleId;
        this.actId = actId;
        this.actName = actName;
        this.keyword = keyword;
        this.requirements = requirements;
        this.riskLevel = riskLevel;
        this.suggestion = suggestion;
        this.edited = edited;
    }

    // getters (no setters – immutable)
    public Long getRuleId() { return ruleId; }
    public Long getActId() { return actId; }
    public String getActName() { return actName; }
    public String getKeyword() { return keyword; }
    public String getRequirements() { return requirements; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getSuggestion() { return suggestion; }
    public Boolean getEdited() { return edited; }
}