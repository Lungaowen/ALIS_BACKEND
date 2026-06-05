package za.ac.alis.legal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.ac.alis.core.dto.SearchResultDTO;
import za.ac.alis.legal.service.SearchService;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SearchResultDTO> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        return ResponseEntity.ok(searchService.search(query.trim(), safePage, safePageSize));
    }
}
