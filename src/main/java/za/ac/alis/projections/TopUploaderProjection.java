package za.ac.alis.projections;

import za.ac.alis.enums.Role;

/**
 * Interface projection for top uploader report.
 */
public interface TopUploaderProjection {
    Long   getClientId();
    String getFullName();
    String getEmail();
    Role   getRole();
    Long   getDocumentCount();
}
