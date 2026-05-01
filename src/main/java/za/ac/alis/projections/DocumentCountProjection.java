package za.ac.alis.projections;

/** Interface projection for per-client document count. */
public interface DocumentCountProjection {
    Long   getClientId();
    String getFullName();
    Long   getDocumentCount();
}
