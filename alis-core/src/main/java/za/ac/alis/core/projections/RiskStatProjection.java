package za.ac.alis.core.projections;

import za.ac.alis.core.enums.RiskLevel;

/** Interface projection for risk distribution chart. */
public interface RiskStatProjection {
    RiskLevel getRiskLevel();
    Long      getCount();
}
