package za.ac.alis.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "legal_practitioner")
public class LegalPractitioner extends Client {

    private String barNumber;
    private String lawFirm;

    public String getBarNumber() {
        return barNumber;
    }

    public void setBarNumber(String barNumber) {
        this.barNumber = barNumber;
    }

    public String getLawFirm() {
        return lawFirm;
    }

    public void setLawFirm(String lawFirm) {
        this.lawFirm = lawFirm;
    }
}