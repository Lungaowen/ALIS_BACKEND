package za.ac.alis.core.dto;

import za.ac.alis.core.projections.RiskStatProjection;

public class RiskStatDTO {

    private String riskLevel;
    private Long   count;

    public static RiskStatDTO from(RiskStatProjection p) {
        RiskStatDTO dto = new RiskStatDTO();
        dto.riskLevel = p.getRiskLevel() != null ? p.getRiskLevel().name() : null;
        dto.count     = p.getCount();
        return dto;
    }

    public RiskStatDTO() {}

    public String getRiskLevel() { return riskLevel; }
    public Long   getCount()     { return count; }
}
