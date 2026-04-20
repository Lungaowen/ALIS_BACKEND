package za.ac.alis.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.alis.dto.AdminClientDTO;
import za.ac.alis.enums.Role;
import za.ac.alis.projections.ClientDetailProjection;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.service.AdminClientService;
import za.ac.alis.dto.TopUploaderDTO;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/clients")
// @PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientRepository   clientRepository;
    private final AdminClientService adminClientService;

    public ClientController(ClientRepository clientRepository,
                            AdminClientService adminClientService) {
        this.clientRepository   = clientRepository;
        this.adminClientService = adminClientService;
    }

    // GET /api/admin/clients?page=0&size=20&sort=createdAt&dir=desc
    @GetMapping
    public ResponseEntity<Page<ClientDetailProjection>> getAllClients(
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "20")        int    size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc")      String dir) {

        Sort.Direction direction = dir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ResponseEntity.ok(
                clientRepository.findAllClients(PageRequest.of(page, size, Sort.by(direction, sort)))
        );
    }

    // GET /api/admin/clients/{id}
    @GetMapping("/{id}")
    public ResponseEntity<AdminClientDTO.ClientDetail> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(adminClientService.getClientById(id));
    }

    // POST /api/admin/clients/filter
    @PostMapping("/filter")
    public ResponseEntity<Page<AdminClientDTO.ClientDetail>> filterClients(
            @RequestBody AdminClientDTO.FilterRequest filter,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminClientService.filterClients(
                filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
    }

    // GET /api/admin/clients/by-role?role=LEGAL_PRACTITIONER
    @GetMapping("/by-role")
    public ResponseEntity<Page<ClientDetailProjection>> getByRole(
            @RequestParam Role role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                clientRepository.findClientsByRole(role,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    // GET /api/admin/clients/by-date?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
    @GetMapping("/by-date")
    public ResponseEntity<Page<ClientDetailProjection>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                clientRepository.findClientsByDateRange(from, to,
                        PageRequest.of(page, size))
        );
    }

    // GET /api/admin/clients/{id}/document-count
    @GetMapping("/{id}/document-count")
    public ResponseEntity<Long> getDocumentCount(@PathVariable Long id) {
        return ResponseEntity.ok(clientRepository.countDocumentsByClientId(id));
    }

    // PUT /api/admin/clients/{id}
    @PutMapping("/{id}")
    public ResponseEntity<AdminClientDTO.ClientDetail> updateClient(
            @PathVariable Long id,
            @RequestBody AdminClientDTO.UpdateRequest request) {
        return ResponseEntity.ok(adminClientService.updateClient(id, request));
    }

    // DELETE /api/admin/clients/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<AdminClientDTO.DeleteResponse> deleteClient(@PathVariable Long id) {
        return ResponseEntity.ok(adminClientService.deleteClient(id));
    }

    // ── REPORTS ───────────────────────────────────────────────────────────────

    // GET /api/admin/clients/reports/summary
    @GetMapping("/reports/summary")
    public ResponseEntity<AdminClientDTO.ClientSummaryStats> getSummaryStats() {
        return ResponseEntity.ok(adminClientService.getSummaryStats());
    }

    // GET /api/admin/clients/reports/role-distribution
    @GetMapping("/reports/role-distribution")
    public ResponseEntity<AdminClientDTO.RoleDistributionReport> getRoleDistribution() {
        return ResponseEntity.ok(adminClientService.getRoleDistribution());
    }

    // GET /api/admin/clients/reports/registration-trend?months=12
    @GetMapping("/reports/registration-trend")
    public ResponseEntity<AdminClientDTO.RegistrationTrendReport> getRegistrationTrend(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(adminClientService.getRegistrationTrend(months));
    }

    // GET /api/admin/clients/reports/top-uploaders?page=0&size=10
    @GetMapping("/reports/top-uploaders")
    public ResponseEntity<Page<TopUploaderDTO>> getTopUploaders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientService.getTopUploaders(PageRequest.of(page, size)));
    }

    // GET /api/admin/clients/reports/inactive
    @GetMapping("/reports/inactive")
    public ResponseEntity<List<ClientDetailProjection>> getInactiveClients() {
        return ResponseEntity.ok(clientRepository.findClientsWithNoDocuments());
    }
}
