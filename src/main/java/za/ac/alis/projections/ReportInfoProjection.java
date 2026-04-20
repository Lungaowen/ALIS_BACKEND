package za.ac.alis.projections;

import za.ac.alis.enums.AnalysisStatus;
import za.ac.alis.enums.RiskLevel;
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
