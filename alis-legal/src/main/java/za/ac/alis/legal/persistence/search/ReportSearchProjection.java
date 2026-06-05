package za.ac.alis.legal.persistence.search;

import java.time.LocalDateTime;

public interface ReportSearchProjection {

    Long getReportId();

    String getRiskLevel();

    String getAnalysisStatus();

    String getAiRecommendation();

    String getAiExplanation();

    Long getDocumentId();

    String getDocumentTitle();

    Long getClientId();

    LocalDateTime getGeneratedAt();

    Double getRank();
}
