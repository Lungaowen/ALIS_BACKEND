package za.ac.alis.dto;

import za.ac.alis.projections.LegalPractitionerProjection;
import java.time.format.DateTimeFormatter;

/** Frontend-safe LegalPractitioner response. Built from LegalPractitionerProjection. */
public class LegalPractitionerDTO {

    private Long   clientId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String createdAt;
    private String barNumber;
    private String lawFirm;

    public static LegalPractitionerDTO from(LegalPractitionerProjection p) {
        LegalPractitionerDTO dto = new LegalPractitionerDTO();
        dto.clientId  = p.getClientId();
        dto.fullName  = p.getFullName();
        dto.email     = p.getEmail();
        dto.username  = p.getUsername();
        dto.role      = p.getRole() != null ? p.getRole().name() : null;
        dto.createdAt = p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        dto.barNumber = p.getBarNumber();
        dto.lawFirm   = p.getLawFirm();
        return dto;
    }

    public LegalPractitionerDTO() {}

    public Long   getClientId()  { return clientId; }
    public String getFullName()  { return fullName; }
    public String getEmail()     { return email; }
    public String getUsername()  { return username; }
    public String getRole()      { return role; }
    public String getCreatedAt() { return createdAt; }
    public String getBarNumber() { return barNumber; }
    public String getLawFirm()   { return lawFirm; }
}
