package za.ac.alis.core.projections;

import za.ac.alis.core.enums.AnalysisStatus;
import za.ac.alis.core.enums.RiskLevel;
import java.time.LocalDateTime;

/**
 * Interface projection for summary report rows.
 */
public interface ReportInfoProjection {
    Long           getReportId();
    Long           getDocumentId();
    String         getDocumentTitle();
    RiskLevel      getRiskLevel();
    AnalysisStatus getAnalysisStatus();
    String         getAiRecommendation();
    LocalDateTime  getGeneratedAt();
    String         getModelVersion();
}
