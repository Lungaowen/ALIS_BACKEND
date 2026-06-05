package za.ac.alis.core.projections;

import za.ac.alis.core.enums.Role;

/** Interface projection for role distribution chart. */
public interface RoleStatProjection {
    Role getRole();
    Long getCount();
}
