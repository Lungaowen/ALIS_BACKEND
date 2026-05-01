package za.ac.alis.dto;

import java.util.List;

public class AdminDashboardResponseDTO {

    private DashboardStatsDTO       stats;
    private List<ClientActivityDTO> clients;
    private List<DocumentInfoDTO>   recentDocuments;
    private List<ReportInfoDTO>     reports;
    private List<RoleStatDTO>       roleDistribution;
    private List<RiskStatDTO>       riskDistribution;
    private List<MonthlyUploadDTO>  uploadTrend;

    public DashboardStatsDTO getStats() { return stats; }
    public void setStats(DashboardStatsDTO stats) { this.stats = stats; }

    public List<ClientActivityDTO> getClients() { return clients; }
    public void setClients(List<ClientActivityDTO> clients) { this.clients = clients; }

    public List<DocumentInfoDTO> getRecentDocuments() { return recentDocuments; }
    public void setRecentDocuments(List<DocumentInfoDTO> recentDocuments) { this.recentDocuments = recentDocuments; }

    public List<ReportInfoDTO> getReports() { return reports; }
    public void setReports(List<ReportInfoDTO> reports) { this.reports = reports; }

    public List<RoleStatDTO> getRoleDistribution() { return roleDistribution; }
    public void setRoleDistribution(List<RoleStatDTO> roleDistribution) { this.roleDistribution = roleDistribution; }

    public List<RiskStatDTO> getRiskDistribution() { return riskDistribution; }
    public void setRiskDistribution(List<RiskStatDTO> riskDistribution) { this.riskDistribution = riskDistribution; }

    public List<MonthlyUploadDTO> getUploadTrend() { return uploadTrend; }
    public void setUploadTrend(List<MonthlyUploadDTO> uploadTrend) { this.uploadTrend = uploadTrend; }
}
