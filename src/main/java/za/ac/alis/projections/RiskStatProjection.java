package za.ac.alis.projections;

import za.ac.alis.enums.RiskLevel;

/** Interface projection for risk distribution chart. */
public interface RiskStatProjection {
    RiskLevel getRiskLevel();
    Long      getCount();
}
