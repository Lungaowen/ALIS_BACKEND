package za.ac.alis.core.projections;

import za.ac.alis.core.enums.Role;

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
