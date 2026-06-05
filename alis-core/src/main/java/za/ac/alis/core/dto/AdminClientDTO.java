package za.ac.alis.core.dto;

import za.ac.alis.core.enums.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminClientDTO {

    // ─────────────────────────────────────────────
    // CLIENT DETAIL
    // ─────────────────────────────────────────────
    public static class ClientDetail {
        private Long          clientId;
        private String        fullName;
        private String        email;
        private String        username;
        private Role          role;
        private LocalDateTime createdAt;
        private Long          documentsUploaded;

        // Role-specific extras — set by service after query
        private String barNumber;
        private String lawFirm;
        private String companyName;
        private String dealSpecialty;

        // ✅ Constructor used by JPQL new expression (6 args)
        public ClientDetail(Long clientId, String fullName, String email,
                            String username, Role role, LocalDateTime createdAt) {
            this.clientId  = clientId;
            this.fullName  = fullName;
            this.email     = email;
            this.username  = username;
            this.role      = role;
            this.createdAt = createdAt;
        }

        public ClientDetail() {}

        public Long          getClientId()          { return clientId; }
        public void          setClientId(Long v)    { this.clientId = v; }
        public String        getFullName()           { return fullName; }
        public void          setFullName(String v)  { this.fullName = v; }
        public String        getEmail()              { return email; }
        public void          setEmail(String v)     { this.email = v; }
        public String        getUsername()           { return username; }
        public void          setUsername(String v)  { this.username = v; }
        public Role          getRole()               { return role; }
        public void          setRole(Role v)        { this.role = v; }
        public LocalDateTime getCreatedAt()          { return createdAt; }
        public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }
        public Long          getDocumentsUploaded()  { return documentsUploaded; }
        public void          setDocumentsUploaded(Long v) { this.documentsUploaded = v; }
        public String        getBarNumber()          { return barNumber; }
        public void          setBarNumber(String v)  { this.barNumber = v; }
        public String        getLawFirm()            { return lawFirm; }
        public void          setLawFirm(String v)   { this.lawFirm = v; }
        public String        getCompanyName()        { return companyName; }
        public void          setCompanyName(String v){ this.companyName = v; }
        public String        getDealSpecialty()      { return dealSpecialty; }
        public void          setDealSpecialty(String v){ this.dealSpecialty = v; }
    }

    // ─────────────────────────────────────────────
    // DOCUMENT COUNT ENTRY
    // ─────────────────────────────────────────────
    public static class DocumentCountEntry {
        private Long   clientId;
        private String fullName;
        private Long   documentCount;

        public DocumentCountEntry(Long clientId, String fullName, Long documentCount) {
            this.clientId      = clientId;
            this.fullName      = fullName;
            this.documentCount = documentCount;
        }

        public Long   getClientId()      { return clientId; }
        public String getFullName()      { return fullName; }
        public Long   getDocumentCount() { return documentCount; }
    }

    // ─────────────────────────────────────────────
    // ROLE COUNT
    // ─────────────────────────────────────────────
    public static class RoleCount {
        private Role role;
        private Long count;

        public RoleCount(Role role, Long count) {
            this.role  = role;
            this.count = count;
        }

        public Role getRole()  { return role; }
        public Long getCount() { return count; }
    }

    // ─────────────────────────────────────────────
    // MONTHLY COUNT
    // ─────────────────────────────────────────────
    public static class MonthlyCount {
        private int  year;
        private int  month;
        private Long count;

        // FUNCTION() returns Integer — must match
        public MonthlyCount(Integer year, Integer month, Long count) {
            this.year  = year;
            this.month = month;
            this.count = count;
        }

        public int  getYear()  { return year; }
        public int  getMonth() { return month; }
        public Long getCount() { return count; }
    }

    // ─────────────────────────────────────────────
    // TOP UPLOADER REPORT
    // ─────────────────────────────────────────────
    public static class TopUploaderReport {
        private Long   clientId;
        private String fullName;
        private String email;
        private Role   role;
        private Long   documentCount;

        public TopUploaderReport(Long clientId, String fullName, String email,
                                 Role role, Long documentCount) {
            this.clientId      = clientId;
            this.fullName      = fullName;
            this.email         = email;
            this.role          = role;
            this.documentCount = documentCount;
        }

        public Long   getClientId()      { return clientId; }
        public String getFullName()      { return fullName; }
        public String getEmail()         { return email; }
        public Role   getRole()          { return role; }
        public Long   getDocumentCount() { return documentCount; }
    }

    // ─────────────────────────────────────────────
    // ROLE DISTRIBUTION REPORT (assembled in service)
    // ─────────────────────────────────────────────
    public static class RoleDistributionReport {
        private Map<String, Long> countByRole;
        private Long              totalClients;

        public Map<String, Long> getCountByRole()              { return countByRole; }
        public void              setCountByRole(Map<String, Long> v) { this.countByRole = v; }
        public Long              getTotalClients()              { return totalClients; }
        public void              setTotalClients(Long v)        { this.totalClients = v; }
    }

    // ─────────────────────────────────────────────
    // REGISTRATION TREND REPORT (assembled in service)
    // ─────────────────────────────────────────────
    public static class RegistrationTrendReport {
        private List<MonthlyCount> trend;

        public List<MonthlyCount> getTrend()              { return trend; }
        public void               setTrend(List<MonthlyCount> v) { this.trend = v; }
    }

    // ─────────────────────────────────────────────
    // CLIENT SUMMARY STATS (assembled in service)
    // ─────────────────────────────────────────────
    public static class ClientSummaryStats {
        private Long totalClients;
        private Long totalUsers;
        private Long totalLegalPractitioners;
        private Long totalDealMakers;
        private Long totalDocumentsUploaded;
        private Long clientsRegisteredLast30Days;
        private Long clientsWithNoDocuments;

        public Long getTotalClients()                    { return totalClients; }
        public void setTotalClients(Long v)              { this.totalClients = v; }
        public Long getTotalUsers()                      { return totalUsers; }
        public void setTotalUsers(Long v)                { this.totalUsers = v; }
        public Long getTotalLegalPractitioners()         { return totalLegalPractitioners; }
        public void setTotalLegalPractitioners(Long v)   { this.totalLegalPractitioners = v; }
        public Long getTotalDealMakers()                 { return totalDealMakers; }
        public void setTotalDealMakers(Long v)           { this.totalDealMakers = v; }
        public Long getTotalDocumentsUploaded()          { return totalDocumentsUploaded; }
        public void setTotalDocumentsUploaded(Long v)    { this.totalDocumentsUploaded = v; }
        public Long getClientsRegisteredLast30Days()     { return clientsRegisteredLast30Days; }
        public void setClientsRegisteredLast30Days(Long v){ this.clientsRegisteredLast30Days = v; }
        public Long getClientsWithNoDocuments()          { return clientsWithNoDocuments; }
        public void setClientsWithNoDocuments(Long v)    { this.clientsWithNoDocuments = v; }
    }

    // ─────────────────────────────────────────────
    // UPDATE REQUEST
    // ─────────────────────────────────────────────
    public static class UpdateRequest {
        private String fullName;
        private String email;
        private String username;
        private Role   role;
        private String barNumber;
        private String lawFirm;
        private String companyName;
        private String dealSpecialty;

        public String getFullName()       { return fullName; }
        public void   setFullName(String v){ this.fullName = v; }
        public String getEmail()          { return email; }
        public void   setEmail(String v)  { this.email = v; }
        public String getUsername()       { return username; }
        public void   setUsername(String v){ this.username = v; }
        public Role   getRole()           { return role; }
        public void   setRole(Role v)     { this.role = v; }
        public String getBarNumber()      { return barNumber; }
        public void   setBarNumber(String v){ this.barNumber = v; }
        public String getLawFirm()        { return lawFirm; }
        public void   setLawFirm(String v){ this.lawFirm = v; }
        public String getCompanyName()    { return companyName; }
        public void   setCompanyName(String v){ this.companyName = v; }
        public String getDealSpecialty()  { return dealSpecialty; }
        public void   setDealSpecialty(String v){ this.dealSpecialty = v; }
    }

    // ─────────────────────────────────────────────
    // FILTER REQUEST
    // ─────────────────────────────────────────────
    public static class FilterRequest {
        private Role          role;
        private LocalDateTime registeredFrom;
        private LocalDateTime registeredTo;
        private String        searchQuery;

        public Role          getRole()           { return role; }
        public void          setRole(Role v)     { this.role = v; }
        public LocalDateTime getRegisteredFrom() { return registeredFrom; }
        public void          setRegisteredFrom(LocalDateTime v) { this.registeredFrom = v; }
        public LocalDateTime getRegisteredTo()   { return registeredTo; }
        public void          setRegisteredTo(LocalDateTime v)   { this.registeredTo = v; }
        public String        getSearchQuery()    { return searchQuery; }
        public void          setSearchQuery(String v) { this.searchQuery = v; }
    }

    // ─────────────────────────────────────────────
    // DELETE RESPONSE
    // ─────────────────────────────────────────────
    public static class DeleteResponse {
        private Long   clientId;
        private String message;

        public DeleteResponse(Long clientId, String message) {
            this.clientId = clientId;
            this.message  = message;
        }

        public Long   getClientId() { return clientId; }
        public String getMessage()  { return message; }
    }
}
