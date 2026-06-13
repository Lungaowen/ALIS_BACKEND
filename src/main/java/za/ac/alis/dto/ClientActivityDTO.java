package za.ac.alis.dto;

import za.ac.alis.enums.Role;
import za.ac.alis.projections.ClientActivityProjection;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientActivityDTO {

    private Long   clientId;
    private String fullName;
    private String email;
    private String role;
    private String registeredAt;
    private Long   documentCount;
    private List<DocumentInfoDTO> recentDocuments;  // new field

    public static ClientActivityDTO from(ClientActivityProjection p) {
        ClientActivityDTO dto = new ClientActivityDTO();
        dto.clientId = p.getClientId();
        dto.fullName = p.getFullName();
        dto.email = p.getEmail();
        dto.role = p.getRole() != null ? p.getRole().name() : null;
        dto.registeredAt = p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
        dto.documentCount = p.getDocumentCount();
        return dto;
    }

    // Getters and Setters (add for recentDocuments)
    public List<DocumentInfoDTO> getRecentDocuments() { return recentDocuments; }
    public void setRecentDocuments(List<DocumentInfoDTO> recentDocuments) { this.recentDocuments = recentDocuments; }

    // other getters/setters...
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }
    public Long getDocumentCount() { return documentCount; }
    public void setDocumentCount(Long documentCount) { this.documentCount = documentCount; }
}