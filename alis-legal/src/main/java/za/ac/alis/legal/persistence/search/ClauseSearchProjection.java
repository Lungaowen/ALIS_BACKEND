package za.ac.alis.legal.persistence.search;

public interface ClauseSearchProjection {

    Long getClauseId();

    String getClauseText();

    String getRiskLevel();

    String getRiskReason();

    Integer getPageNumber();

    Long getDocumentId();

    String getDocumentTitle();

    Long getClientId();

    Double getRank();
}
