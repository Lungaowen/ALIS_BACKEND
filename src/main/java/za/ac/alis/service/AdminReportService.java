package za.ac.alis.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.dto.AdminReportDTO;
import za.ac.alis.dto.ClientActivityDTO;
import za.ac.alis.dto.DocumentInfoDTO;
import za.ac.alis.projections.ClientActivityProjection;
import za.ac.alis.projections.DocumentInfoProjection;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.SummaryReportRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminReportService {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final SummaryReportRepository summaryReportRepository;

    public AdminReportService(ClientRepository clientRepository,
                              DocumentRepository documentRepository,
                              SummaryReportRepository summaryReportRepository) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
        this.summaryReportRepository = summaryReportRepository;
    }

    public AdminReportDTO getFullReport() {
        AdminReportDTO report = new AdminReportDTO();

        report.setTotalClients(clientRepository.count());
        report.setTotalDocuments(documentRepository.count());
        report.setTotalReports(summaryReportRepository.count());
        report.setActiveClients(documentRepository.countDistinctClientIds());

        // Fetch client activities using optimized projection query
        List<ClientActivityProjection> activitiesProj = clientRepository.findClientActivities(null);
        List<ClientActivityDTO> activities = activitiesProj.stream()
                .map(ClientActivityDTO::from)
                .collect(Collectors.toList());

        // For each client, fetch recent documents using projection
        for (ClientActivityDTO activity : activities) {
            List<DocumentInfoProjection> docsProj = documentRepository
                    .findRecentDocumentsByClientId(activity.getClientId());
            List<DocumentInfoDTO> docList = docsProj.stream()
                    .map(DocumentInfoDTO::from)
                    .collect(Collectors.toList());
           activity.setRecentDocuments(docList); // You may need to add this field to ClientActivityDTO
        }

        report.setClientActivities(activities);
        return report;
    }
}