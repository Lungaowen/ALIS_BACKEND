package za.ac.alis.core.dto;

import za.ac.alis.core.enums.Role;

public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
    private String barNumber;
    private String lawFirm;
    private String companyName;
    private String dealSpecialty;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getBarNumber() { return barNumber; }
    public void setBarNumber(String barNumber) { this.barNumber = barNumber; }

    public String getLawFirm() { return lawFirm; }
    public void setLawFirm(String lawFirm) { this.lawFirm = lawFirm; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getDealSpecialty() { return dealSpecialty; }
    public void setDealSpecialty(String dealSpecialty) { this.dealSpecialty = dealSpecialty; }
}
