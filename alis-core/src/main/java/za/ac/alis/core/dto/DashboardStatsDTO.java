package za.ac.alis.core.dto;

public class DashboardStatsDTO {

    private long totalClients;
    private long totalDocuments;
    private long totalReports;
    private long activeClients;
    private long pendingDocuments;
    private long failedDocuments;
    private long processedDocuments;
    private long highRiskReports;

    public long getTotalClients()       { return totalClients; }
    public void setTotalClients(long v) { this.totalClients = v; }

    public long getTotalDocuments()       { return totalDocuments; }
    public void setTotalDocuments(long v) { this.totalDocuments = v; }

    public long getTotalReports()       { return totalReports; }
    public void setTotalReports(long v) { this.totalReports = v; }

    public long getActiveClients()       { return activeClients; }
    public void setActiveClients(long v) { this.activeClients = v; }

    public long getPendingDocuments()       { return pendingDocuments; }
    public void setPendingDocuments(long v) { this.pendingDocuments = v; }

    public long getFailedDocuments()       { return failedDocuments; }
    public void setFailedDocuments(long v) { this.failedDocuments = v; }

    public long getProcessedDocuments()       { return processedDocuments; }
    public void setProcessedDocuments(long v) { this.processedDocuments = v; }

    public long getHighRiskReports()       { return highRiskReports; }
    public void setHighRiskReports(long v) { this.highRiskReports = v; }
}
