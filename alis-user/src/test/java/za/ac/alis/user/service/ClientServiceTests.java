package za.ac.alis.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import za.ac.alis.core.dto.ClientProfileUpdateRequest;
import za.ac.alis.core.enums.ActionType;
import za.ac.alis.core.enums.Role;
import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.port.AuditLogPort;
import za.ac.alis.core.port.DocumentCleanupPort;
import za.ac.alis.user.persistence.ClientRepository;

class ClientServiceTests {

    @Test
    void updatesOwnProfileIncludingPasswordWhenCurrentPasswordMatches() {
        ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
        AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);
        DocumentCleanupPort documentCleanupPort = Mockito.mock(DocumentCleanupPort.class);
        ClientService service = new ClientService(clientRepository, auditLogPort, documentCleanupPort);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Client client = new Client();
        client.setFullName("Old Name");
        client.setEmail("user@example.com");
        client.setUsername("old_user");
        client.setRole(Role.USER);
        client.setPasswordHash(encoder.encode("old-password"));

        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest();
        request.setFullName("New Name");
        request.setUsername("new_user");
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(clientRepository.existsByUsername("new_user")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client updated = service.updateOwnProfile(7L, request);

        assertThat(updated.getFullName()).isEqualTo("New Name");
        assertThat(updated.getUsername()).isEqualTo("new_user");
        assertThat(encoder.matches("new-password", updated.getPasswordHash())).isTrue();
        verify(auditLogPort).logClientAction(eq(updated), eq(ActionType.USER_UPDATED), any(String.class), eq("SELF_SERVICE"));
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordIsIncorrect() {
        ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
        AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);
        DocumentCleanupPort documentCleanupPort = Mockito.mock(DocumentCleanupPort.class);
        ClientService service = new ClientService(clientRepository, auditLogPort, documentCleanupPort);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Client client = new Client();
        client.setPasswordHash(encoder.encode("right-password"));

        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest();
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("new-password");

        when(clientRepository.findById(3L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.updateOwnProfile(3L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void deactivatesOwnAccount() {
        ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
        AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);
        DocumentCleanupPort documentCleanupPort = Mockito.mock(DocumentCleanupPort.class);
        ClientService service = new ClientService(clientRepository, auditLogPort, documentCleanupPort);

        Client client = new Client();
        client.setEmail("user@example.com");
        client.setActive(true);

        when(clientRepository.findById(9L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client deactivated = service.deactivateOwnAccount(9L);

        assertThat(deactivated.isActive()).isFalse();
        assertThat(deactivated.getDeactivatedAt()).isNotNull();
        verify(auditLogPort).logClientAction(eq(deactivated), eq(ActionType.USER_UPDATED), any(String.class), eq("SELF_SERVICE"));
    }

    @Test
    void deletesOwnAccountDocumentsAndAuditLogsBeforeRemovingClient() {
        ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
        AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);
        DocumentCleanupPort documentCleanupPort = Mockito.mock(DocumentCleanupPort.class);
        ClientService service = new ClientService(clientRepository, auditLogPort, documentCleanupPort);

        Client client = new Client();

        when(clientRepository.findById(4L)).thenReturn(Optional.of(client));

        service.deleteOwnAccount(4L);

        verify(documentCleanupPort).deleteAllDocumentsForClient(4L);
        verify(auditLogPort).deleteByClientId(4L);
        verify(clientRepository).delete(client);
    }
}
