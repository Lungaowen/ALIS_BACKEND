package za.ac.alis.user.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.port.AuditLogPort;
import za.ac.alis.core.port.DocumentCleanupPort;
import za.ac.alis.user.persistence.ClientRepository;
import za.ac.alis.user.persistence.DealMakerRepository;
import za.ac.alis.user.persistence.LegalPractitionerRepository;

class AdminClientServiceTests {

    @Test
    void deletesClientDocumentsAndAuditLogsBeforeRemovingClient() {
        ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
        DealMakerRepository dealMakerRepository = Mockito.mock(DealMakerRepository.class);
        LegalPractitionerRepository legalPractitionerRepository = Mockito.mock(LegalPractitionerRepository.class);
        DocumentCleanupPort documentCleanupPort = Mockito.mock(DocumentCleanupPort.class);
        AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);

        AdminClientService service = new AdminClientService(
                clientRepository,
                dealMakerRepository,
                legalPractitionerRepository,
                documentCleanupPort,
                auditLogPort
        );

        Client client = new Client();

        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

        service.deleteClient(5L);

        verify(documentCleanupPort).deleteAllDocumentsForClient(5L);
        verify(auditLogPort).deleteByClientId(5L);
        verify(clientRepository).delete(client);
    }
}
