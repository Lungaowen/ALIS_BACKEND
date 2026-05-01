package za.ac.alis.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import za.ac.alis.enums.*;

@Entity
@Table(name = "act")
public class Act {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actId;

    @Column(nullable = false, unique = true)
    private String actName;

    private String actNumber;
    private Integer actYear;
    private String actSection;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String jurisdiction = "South Africa";

    @OneToMany(mappedBy = "act", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LawRul> lawRules = new ArrayList<>();

    // Getters and Setters
    public Long getActId() { return actId; }
    public void setActId(Long actId) { this.actId = actId; }

    public String getActName() { return actName; }
    public void setActName(String actName) { this.actName = actName; }

    public String getActNumber() { return actNumber; }
    public void setActNumber(String actNumber) { this.actNumber = actNumber; }

    public Integer getActYear() { return actYear; }
    public void setActYear(Integer actYear) { this.actYear = actYear; }

    public String getActSection() { return actSection; }
    public void setActSection(String actSection) { this.actSection = actSection; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public List<LawRul> getLawRules() { return lawRules; }
    public void setLawRules(List<LawRul> lawRules) { this.lawRules = lawRules; }
}