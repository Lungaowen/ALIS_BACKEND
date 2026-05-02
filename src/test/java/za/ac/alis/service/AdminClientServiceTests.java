package za.ac.alis.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import za.ac.alis.entities.Client;
import za.ac.alis.entities.Document;
import za.ac.alis.repo.AuditLogRepository;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.repo.DealMakerRepository;
import za.ac.alis.repo.LegalPractitionerRepository;

class AdminClientServiceTests {

    @Test
    void deletesClientDocumentsAndAuditLogsBeforeRemovingClient() {
        ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
        DealMakerRepository dealMakerRepository = Mockito.mock(DealMakerRepository.class);
        LegalPractitionerRepository legalPractitionerRepository = Mockito.mock(LegalPractitionerRepository.class);
        DocumentService documentService = Mockito.mock(DocumentService.class);
        AuditLogRepository auditLogRepository = Mockito.mock(AuditLogRepository.class);

        AdminClientService service = new AdminClientService(
                clientRepository,
                dealMakerRepository,
                legalPractitionerRepository,
                documentService,
                auditLogRepository
        );

        Client client = new Client();
        Document document = new Document();
        document.setDocumentId(11L);

        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));
        when(documentService.getDocumentsByClientId(5L)).thenReturn(List.of(document));

        service.deleteClient(5L);

        verify(documentService).deleteDocument(11L);
        verify(auditLogRepository).deleteByClientClientId(5L);
        verify(clientRepository).delete(client);
    }
}
