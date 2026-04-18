package za.ac.alis.controller;

import za.ac.alis.dto.AuditLogDTO;
import za.ac.alis.entities.AuditLog;
import za.ac.alis.repo.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/audit")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Get the most recent 20 logs (best for dashboard)
    @GetMapping("/recent")
    public List<AuditLogDTO> getRecentLogs() {
        List<AuditLog> logs = auditLogRepository.findTop20ByOrderByCreatedAtDesc();
        return convertToDTOList(logs);
    }

    // Get all logs
    @GetMapping
    public List<AuditLogDTO> getAllLogs() {
    return auditLogRepository.findAllLogs()
            .stream()
            .map(this::toDTO)
            .toList();
}

    // Get logs for a specific client
    @GetMapping("/client/{clientId}")
    public List<AuditLogDTO> getLogsByClient(@PathVariable Long clientId) {
        List<AuditLog> logs = auditLogRepository.findByClient_ClientIdOrderByCreatedAtDesc(clientId);
        return convertToDTOList(logs);
    }

    // Safe conversion - prevents LazyInitializationException
    private List<AuditLogDTO> convertToDTOList(List<AuditLog> logs) {
        return logs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private AuditLogDTO toDTO(AuditLog log) {
        return new AuditLogDTO(
            log.getLogId(),
            log.getActionType(),
            log.getDescription(),
            log.getIpAddress(),
            log.getCreatedAt(),
            log.getClient() != null ? log.getClient().getClientId() : null,
            log.getAdmin() != null ? log.getAdmin().getAdminId() : null,
            log.getDocument() != null ? log.getDocument().getDocumentId() : null
        );
    }
}