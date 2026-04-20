package za.ac.alis.dto;

import java.util.List;

public class AdminReportDTO {

    private long totalClients;
    private long totalDocuments;
    private long totalReports;
    private long activeClients;        // clients who uploaded at least one document

    private List<ClientActivityDTO> clientActivities;

    // Getters & Setters
    public long getTotalClients() { return totalClients; }
    public void setTotalClients(long totalClients) { this.totalClients = totalClients; }

    public long getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(long totalDocuments) { this.totalDocuments = totalDocuments; }

    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }

    public long getActiveClients() { return activeClients; }
    public void setActiveClients(long activeClients) { this.activeClients = activeClients; }

    public List<ClientActivityDTO> getClientActivities() { return clientActivities; }
    public void setClientActivities(List<ClientActivityDTO> clientActivities) { this.clientActivities = clientActivities; }
}