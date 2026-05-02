package za.ac.alis.dto;

import java.time.format.DateTimeFormatter;

import za.ac.alis.entities.Client;
import za.ac.alis.entities.DealMaker;
import za.ac.alis.entities.LegalPractitioner;

public class ClientProfileResponse {

    private Long clientId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String createdAt;
    private boolean active;
    private String deactivatedAt;
    private String barNumber;
    private String lawFirm;
    private String companyName;
    private String dealSpecialty;

    public static ClientProfileResponse from(Client client) {
        ClientProfileResponse response = new ClientProfileResponse();
        response.clientId = client.getClientId();
        response.fullName = client.getFullName();
        response.email = client.getEmail();
        response.username = client.getUsername();
        response.role = client.getRole() != null ? client.getRole().name() : null;
        response.createdAt = client.getCreatedAt() != null
                ? client.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
        response.active = client.isActive();
        response.deactivatedAt = client.getDeactivatedAt() != null
                ? client.getDeactivatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
        if (client instanceof LegalPractitioner legalPractitioner) {
            response.barNumber = legalPractitioner.getBarNumber();
            response.lawFirm = legalPractitioner.getLawFirm();
        } else if (client instanceof DealMaker dealMaker) {
            response.companyName = dealMaker.getCompanyName();
            response.dealSpecialty = dealMaker.getDealSpecialty();
        }
        return response;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getDeactivatedAt() {
        return deactivatedAt;
    }

    public String getBarNumber() {
        return barNumber;
    }

    public String getLawFirm() {
        return lawFirm;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDealSpecialty() {
        return dealSpecialty;
    }
}
