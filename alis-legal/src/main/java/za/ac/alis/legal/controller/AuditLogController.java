package za.ac.alis.legal.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.ac.alis.core.dto.AuditLogDTO;
import za.ac.alis.legal.persistence.AuditLogRepository;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // GET /api/admin/audit
    @GetMapping
    public ResponseEntity<List<AuditLogDTO>> getAllLogs() {
        List<AuditLogDTO> logs = auditLogRepository.findAllLogs()
                .stream()
                .map(AuditLogDTO::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    // GET /api/admin/audit/recent?limit=20
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLogDTO>> getRecentLogs(
            @RequestParam(defaultValue = "20") int limit) {
        List<AuditLogDTO> logs = auditLogRepository
                .findRecentLogs(PageRequest.of(0, limit))
                .stream()
                .map(AuditLogDTO::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    // GET /api/admin/audit/client/{clientId}
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AuditLogDTO>> getLogsByClient(@PathVariable Long clientId) {
        List<AuditLogDTO> logs = auditLogRepository
                .findLogsByClientId(clientId)
                .stream()
                .map(AuditLogDTO::from)
                .toList();
        return ResponseEntity.ok(logs);
    }
}
