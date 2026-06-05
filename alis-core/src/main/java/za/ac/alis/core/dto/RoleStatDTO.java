package za.ac.alis.core.dto;

import za.ac.alis.core.projections.RoleStatProjection;

public class RoleStatDTO {

    private String role;
    private Long   count;

    public static RoleStatDTO from(RoleStatProjection p) {
        RoleStatDTO dto = new RoleStatDTO();
        dto.role  = p.getRole() != null ? p.getRole().name() : null;
        dto.count = p.getCount();
        return dto;
    }

    public RoleStatDTO() {}

    public String getRole()  { return role; }
    public Long   getCount() { return count; }
}
