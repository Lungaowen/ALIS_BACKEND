package za.ac.alis.core.dto;

import java.util.List;

public class SearchResultDTO {

    private String query;
    private long totalDocuments;
    private long totalReports;
    private long totalClauses;
    private int page;
    private int pageSize;
    private List<DocumentSearchDTO> documents;
    private List<ReportSearchDTO> reports;
    private List<ClauseSearchDTO> clauses;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getTotalClauses() {
        return totalClauses;
    }

    public void setTotalClauses(long totalClauses) {
        this.totalClauses = totalClauses;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<DocumentSearchDTO> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentSearchDTO> documents) {
        this.documents = documents;
    }

    public List<ReportSearchDTO> getReports() {
        return reports;
    }

    public void setReports(List<ReportSearchDTO> reports) {
        this.reports = reports;
    }

    public List<ClauseSearchDTO> getClauses() {
        return clauses;
    }

    public void setClauses(List<ClauseSearchDTO> clauses) {
        this.clauses = clauses;
    }
}
