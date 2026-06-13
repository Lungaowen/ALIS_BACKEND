package za.ac.alis.dto;

import za.ac.alis.projections.TopUploaderProjection;

public class TopUploaderDTO {

    private Long   clientId;
    private String fullName;
    private String email;
    private String role;
    private Long   documentCount;

    public static TopUploaderDTO from(TopUploaderProjection p) {
        TopUploaderDTO dto = new TopUploaderDTO();
        dto.clientId      = p.getClientId();
        dto.fullName      = p.getFullName();
        dto.email         = p.getEmail();
        dto.role          = p.getRole() != null ? p.getRole().name() : null;
        dto.documentCount = p.getDocumentCount();
        return dto;
    }

    public TopUploaderDTO() {}

    public Long   getClientId()      { return clientId; }
    public String getFullName()      { return fullName; }
    public String getEmail()         { return email; }
    public String getRole()          { return role; }
    public Long   getDocumentCount() { return documentCount; }
}
