package za.ac.alis.core.projections;

/** Interface projection for per-client document count. */
public interface DocumentCountProjection {
    Long   getClientId();
    String getFullName();
    Long   getDocumentCount();
}
