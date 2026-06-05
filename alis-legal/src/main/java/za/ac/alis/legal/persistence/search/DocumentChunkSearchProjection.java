package za.ac.alis.legal.persistence.search;

public interface DocumentChunkSearchProjection {

    Long getChunkId();

    Long getDocumentId();

    String getDocumentTitle();

    Integer getChunkIndex();

    String getChunkText();

    Long getClientId();

    Double getRank();
}
