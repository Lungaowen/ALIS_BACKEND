package za.ac.alis.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.dto.AdminClientDTO;
import za.ac.alis.entities.Client;
import za.ac.alis.entities.DealMaker;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.LegalPractitioner;
import za.ac.alis.enums.Role;
import za.ac.alis.projections.*;
import za.ac.alis.repo.AuditLogRepository;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.repo.DealMakerRepository;
import za.ac.alis.repo.LegalPractitionerRepository;
import za.ac.alis.dto.TopUploaderDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminClientService {

    private final ClientRepository clientRepository;
    private final DealMakerRepository dealMakerRepository;
    private final LegalPractitionerRepository legalPractitionerRepository;
    private final DocumentService documentService;
    private final AuditLogRepository auditLogRepository;

    public AdminClientService(ClientRepository clientRepository,
                              DealMakerRepository dealMakerRepository,
                              LegalPractitionerRepository legalPractitionerRepository,
                              DocumentService documentService,
                              AuditLogRepository auditLogRepository) {
        this.clientRepository = clientRepository;
        this.dealMakerRepository = dealMakerRepository;
        this.legalPractitionerRepository = legalPractitionerRepository;
        this.documentService = documentService;
        this.auditLogRepository = auditLogRepository;
    }

    public AdminClientDTO.ClientDetail getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        AdminClientDTO.ClientDetail detail = new AdminClientDTO.ClientDetail(
                client.getClientId(),
                client.getFullName(),
                client.getEmail(),
                client.getUsername(),
                client.getRole(),
                client.getCreatedAt()
        );
        detail.setDocumentsUploaded(clientRepository.countDocumentsByClientId(id));

        if (client instanceof LegalPractitioner lp) {
            detail.setBarNumber(lp.getBarNumber());
            detail.setLawFirm(lp.getLawFirm());
        } else if (client instanceof DealMaker dm) {
            detail.setCompanyName(dm.getCompanyName());
            detail.setDealSpecialty(dm.getDealSpecialty());
        }
        return detail;
    }

    public Page<AdminClientDTO.ClientDetail> filterClients(AdminClientDTO.FilterRequest filter, Pageable pageable) {
        // Simplified: use the repository filter method and convert projections to DTOs
        Page<ClientDetailProjection> page = clientRepository.findByFilters(
                filter.getRole(),
                filter.getRegisteredFrom(),
                filter.getRegisteredTo(),
                pageable
        );
        return page.map(this::toClientDetail);
    }

    public AdminClientDTO.ClientDetail updateClient(Long id, AdminClientDTO.UpdateRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        client.setFullName(request.getFullName());
        client.setEmail(request.getEmail());
        client.setUsername(request.getUsername());
        if (request.getRole() != null) {
            client.setRole(request.getRole());
        }

        if (client instanceof LegalPractitioner lp && request.getBarNumber() != null) {
            lp.setBarNumber(request.getBarNumber());
            lp.setLawFirm(request.getLawFirm());
        } else if (client instanceof DealMaker dm && request.getCompanyName() != null) {
            dm.setCompanyName(request.getCompanyName());
            dm.setDealSpecialty(request.getDealSpecialty());
        }

        Client saved = clientRepository.save(client);
        return toClientDetail(saved);
    }

    public AdminClientDTO.DeleteResponse deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        List<Document> documents = documentService.getDocumentsByClientId(id);
        for (Document document : documents) {
            documentService.deleteDocument(document.getDocumentId());
        }

        auditLogRepository.deleteByClientClientId(id);
        clientRepository.delete(client);
        return new AdminClientDTO.DeleteResponse(id, "Client deleted successfully");
    }

    public AdminClientDTO.ClientSummaryStats getSummaryStats() {
        AdminClientDTO.ClientSummaryStats stats = new AdminClientDTO.ClientSummaryStats();
        stats.setTotalClients(clientRepository.count());
        stats.setTotalUsers(clientRepository.countByRole(Role.USER));
        stats.setTotalLegalPractitioners(clientRepository.countByRole(Role.LEGAL_PRACTITIONER));
        stats.setTotalDealMakers(clientRepository.countByRole(Role.DEAL_MAKER));
        // Add other stats as needed
        return stats;
    }

    public AdminClientDTO.RoleDistributionReport getRoleDistribution() {
        List<RoleStatProjection> proj = clientRepository.findRoleDistribution();
        Map<String, Long> map = proj.stream()
                .collect(Collectors.toMap(
                        p -> p.getRole().name(),
                        RoleStatProjection::getCount
                ));
        AdminClientDTO.RoleDistributionReport report = new AdminClientDTO.RoleDistributionReport();
        report.setCountByRole(map);
        report.setTotalClients(clientRepository.count());
        return report;
    }

    public AdminClientDTO.RegistrationTrendReport getRegistrationTrend(int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<MonthlyCountProjection> proj = clientRepository.countRegistrationsByMonth(since);
        List<AdminClientDTO.MonthlyCount> trend = proj.stream()
                .map(p -> new AdminClientDTO.MonthlyCount(p.getYear(), p.getMonth(), p.getCount()))
                .collect(Collectors.toList());
        AdminClientDTO.RegistrationTrendReport report = new AdminClientDTO.RegistrationTrendReport();
        report.setTrend(trend);
        return report;
    }

    public Page<TopUploaderDTO> getTopUploaders(Pageable pageable) {
        Page<TopUploaderProjection> page = clientRepository.findTopUploaders(pageable);
        return page.map(TopUploaderDTO::from);
    }

    private AdminClientDTO.ClientDetail toClientDetail(ClientDetailProjection p) {
        AdminClientDTO.ClientDetail dto = new AdminClientDTO.ClientDetail(
                p.getClientId(), p.getFullName(), p.getEmail(),
                p.getUsername(), p.getRole(), p.getCreatedAt()
        );
        dto.setDocumentsUploaded(clientRepository.countDocumentsByClientId(p.getClientId()));
        return dto;
    }

    private AdminClientDTO.ClientDetail toClientDetail(Client client) {
        AdminClientDTO.ClientDetail dto = new AdminClientDTO.ClientDetail(
                client.getClientId(), client.getFullName(), client.getEmail(),
                client.getUsername(), client.getRole(), client.getCreatedAt()
        );
        dto.setDocumentsUploaded(clientRepository.countDocumentsByClientId(client.getClientId()));
        if (client instanceof LegalPractitioner lp) {
            dto.setBarNumber(lp.getBarNumber());
            dto.setLawFirm(lp.getLawFirm());
        } else if (client instanceof DealMaker dm) {
            dto.setCompanyName(dm.getCompanyName());
            dto.setDealSpecialty(dm.getDealSpecialty());
        }
        return dto;
    }
}
