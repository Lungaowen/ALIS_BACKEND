package za.ac.alis.projections;

import za.ac.alis.enums.Role;

/** Interface projection for role distribution chart. */
public interface RoleStatProjection {
    Role getRole();
    Long getCount();
}
