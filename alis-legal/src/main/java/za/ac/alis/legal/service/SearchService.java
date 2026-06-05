package za.ac.alis.legal.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.core.dto.ClauseSearchDTO;
import za.ac.alis.core.dto.DocumentSearchDTO;
import za.ac.alis.core.dto.ReportSearchDTO;
import za.ac.alis.core.dto.SearchResultDTO;
import za.ac.alis.legal.persistence.search.ClauseSearchProjection;
import za.ac.alis.legal.persistence.search.ClauseSearchRepository;
import za.ac.alis.legal.persistence.search.DocumentSearchProjection;
import za.ac.alis.legal.persistence.search.DocumentSearchRepository;
import za.ac.alis.legal.persistence.search.ReportSearchProjection;
import za.ac.alis.legal.persistence.search.ReportSearchRepository;

@Service
public class SearchService {

    private final DocumentSearchRepository documentSearchRepository;
    private final ReportSearchRepository reportSearchRepository;
    private final ClauseSearchRepository clauseSearchRepository;

    public SearchService(DocumentSearchRepository documentSearchRepository,
                         ReportSearchRepository reportSearchRepository,
                         ClauseSearchRepository clauseSearchRepository) {
        this.documentSearchRepository = documentSearchRepository;
        this.reportSearchRepository = reportSearchRepository;
        this.clauseSearchRepository = clauseSearchRepository;
    }

    @Transactional(readOnly = true)
    public SearchResultDTO search(String query, int page, int pageSize) {
        int offset = page * pageSize;
        Long clientId = resolveScopedClientId();

        var documents = documentSearchRepository
                .searchDocuments(query, clientId, pageSize, offset)
                .stream()
                .map(this::toDocumentDTO)
                .toList();

        var reports = reportSearchRepository
                .searchReports(query, clientId, pageSize, offset)
                .stream()
                .map(this::toReportDTO)
                .toList();

        var clauses = clauseSearchRepository
                .searchClauses(query, clientId, pageSize, offset)
                .stream()
                .map(this::toClauseDTO)
                .toList();

        SearchResultDTO result = new SearchResultDTO();
        result.setQuery(query);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotalDocuments(documentSearchRepository.countDocumentSearch(query, clientId));
        result.setTotalReports(reportSearchRepository.countReportSearch(query, clientId));
        result.setTotalClauses(clauseSearchRepository.countClauseSearch(query, clientId));
        result.setDocuments(documents);
        result.setReports(reports);
        result.setClauses(clauses);
        return result;
    }

    private Long resolveScopedClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        if (hasRole(authentication, "ADMIN") || hasRole(authentication, "LEGAL_PRACTITIONER")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String value) {
            return Long.valueOf(value);
        }
        throw new IllegalStateException("Authenticated principal is not a client id");
    }

    private boolean hasRole(Authentication authentication, String role) {
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private DocumentSearchDTO toDocumentDTO(DocumentSearchProjection projection) {
        DocumentSearchDTO dto = new DocumentSearchDTO();
        dto.setDocumentId(projection.getDocumentId());
        dto.setTitle(projection.getTitle());
        dto.setStatus(projection.getStatus());
        dto.setUploadedAt(projection.getUploadedAt());
        dto.setClientId(projection.getClientId());
        dto.setRank(projection.getRank());
        return dto;
    }

    private ReportSearchDTO toReportDTO(ReportSearchProjection projection) {
        ReportSearchDTO dto = new ReportSearchDTO();
        dto.setReportId(projection.getReportId());
        dto.setRiskLevel(projection.getRiskLevel());
        dto.setAnalysisStatus(projection.getAnalysisStatus());
        dto.setAiRecommendation(projection.getAiRecommendation());
        dto.setAiExplanation(projection.getAiExplanation());
        dto.setDocumentId(projection.getDocumentId());
        dto.setDocumentTitle(projection.getDocumentTitle());
        dto.setClientId(projection.getClientId());
        dto.setGeneratedAt(projection.getGeneratedAt());
        dto.setRank(projection.getRank());
        return dto;
    }

    private ClauseSearchDTO toClauseDTO(ClauseSearchProjection projection) {
        ClauseSearchDTO dto = new ClauseSearchDTO();
        dto.setClauseId(projection.getClauseId());
        dto.setClauseText(projection.getClauseText());
        dto.setRiskLevel(projection.getRiskLevel());
        dto.setRiskReason(projection.getRiskReason());
        dto.setPageNumber(projection.getPageNumber());
        dto.setDocumentId(projection.getDocumentId());
        dto.setDocumentTitle(projection.getDocumentTitle());
        dto.setClientId(projection.getClientId());
        dto.setRank(projection.getRank());
        return dto;
    }
}
