package za.ac.alis.projections;

import za.ac.alis.enums.Role;
import java.time.LocalDateTime;

public interface LegalPractitionerProjection {
    Long          getClientId();
    String        getFullName();
    String        getEmail();
    String        getUsername();
    Role          getRole();
    LocalDateTime getCreatedAt();
    String        getBarNumber();
    String        getLawFirm();
}
