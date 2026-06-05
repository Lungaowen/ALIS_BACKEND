package za.ac.alis.core.dto;

import za.ac.alis.core.projections.DealMakerProjection;
import java.time.format.DateTimeFormatter;

/** Frontend-safe DealMaker response. Built from DealMakerProjection. */
public class DealMakerDTO {

    private Long   clientId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String createdAt;
    private String companyName;
    private String dealSpecialty;

    public static DealMakerDTO from(DealMakerProjection p) {
        DealMakerDTO dto = new DealMakerDTO();
        dto.clientId      = p.getClientId();
        dto.fullName      = p.getFullName();
        dto.email         = p.getEmail();
        dto.username      = p.getUsername();
        dto.role          = p.getRole() != null ? p.getRole().name() : null;
        dto.createdAt     = p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        dto.companyName   = p.getCompanyName();
        dto.dealSpecialty = p.getDealSpecialty();
        return dto;
    }

    public DealMakerDTO() {}

    public Long   getClientId()      { return clientId; }
    public String getFullName()      { return fullName; }
    public String getEmail()         { return email; }
    public String getUsername()      { return username; }
    public String getRole()          { return role; }
    public String getCreatedAt()     { return createdAt; }
    public String getCompanyName()   { return companyName; }
    public String getDealSpecialty() { return dealSpecialty; }
}
