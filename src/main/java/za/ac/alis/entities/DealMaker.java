package za.ac.alis.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "dealmaker")
public class DealMaker extends Client {

    private String companyName;
    private String dealSpecialty;

    public DealMaker() {
    }

    public DealMaker(String companyName, String dealSpecialty) {
        this.companyName = companyName;
        this.dealSpecialty = dealSpecialty;
    }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDealSpecialty() {
        return dealSpecialty;
    }
    public void setDealSpecialty(String dealSpecialty) {
        this.dealSpecialty = dealSpecialty;
    }
}