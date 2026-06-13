package za.ac.alis.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.ac.alis.dto.AdminClientDTO;
import za.ac.alis.dto.TopUploaderDTO;
import za.ac.alis.enums.Role;
import za.ac.alis.projections.ClientDetailProjection;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.service.AdminClientService;

@RestController
@RequestMapping("/api/admin/clients")
@PreAuthorize("hasRole('ADMIN')")
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
                clientRepository.findAllClients(
                        PageRequest.of(page, size, Sort.by(direction, sort))));
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
                filter, PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    // GET /api/admin/clients/by-role?role=LEGAL_PRACTITIONER
    @GetMapping("/by-role")
    public ResponseEntity<Page<ClientDetailProjection>> getByRole(
            @RequestParam Role role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                clientRepository.findClientsByRole(role,
                        PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    // GET /api/admin/clients/by-date
    @GetMapping("/by-date")
    public ResponseEntity<Page<ClientDetailProjection>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                clientRepository.findClientsByDateRange(from, to, PageRequest.of(page, size)));
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

    @GetMapping("/reports/summary")
    public ResponseEntity<AdminClientDTO.ClientSummaryStats> getSummaryStats() {
        return ResponseEntity.ok(adminClientService.getSummaryStats());
    }

    @GetMapping("/reports/role-distribution")
    public ResponseEntity<AdminClientDTO.RoleDistributionReport> getRoleDistribution() {
        return ResponseEntity.ok(adminClientService.getRoleDistribution());
    }

    @GetMapping("/reports/registration-trend")
    public ResponseEntity<AdminClientDTO.RegistrationTrendReport> getRegistrationTrend(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(adminClientService.getRegistrationTrend(months));
    }

    @GetMapping("/reports/top-uploaders")
    public ResponseEntity<Page<TopUploaderDTO>> getTopUploaders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminClientService.getTopUploaders(PageRequest.of(page, size)));
    }

    @GetMapping("/reports/inactive")
    public ResponseEntity<List<ClientDetailProjection>> getInactiveClients() {
        return ResponseEntity.ok(clientRepository.findClientsWithNoDocuments());
    }
}
