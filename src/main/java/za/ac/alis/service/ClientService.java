package za.ac.alis.service;

import java.util.List;

import org.springframework.stereotype.Service;

import za.ac.alis.entities.Client;
import za.ac.alis.enums.ActionType;
import za.ac.alis.repo.ClientRepository;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AuditLogService auditLogService;

    public ClientService(ClientRepository clientRepository, AuditLogService auditLogService) {
        this.clientRepository = clientRepository;
        this.auditLogService = auditLogService;
    }

    // =========================
    // REGISTER (from Auth)
    // =========================
    public Client registerClient(Client client) {
        Client saved = clientRepository.save(client);

        auditLogService.logClientAction(
                saved,
                ActionType.USER_CREATED,
                "Client registered: " + saved.getEmail(),
                "SYSTEM"
        );

        return saved;
    }

    // =========================
    // LOGIN
    // =========================
    public Client login(String email, String rawPassword) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Plain text comparison (as requested - no hashing)
        if (!rawPassword.equals(client.getPasswordHash())) {
            auditLogService.logClientAction(
                    client,
                    ActionType.LOGIN,
                    "Failed login attempt for: " + email,
                    "SYSTEM"
            );
            throw new RuntimeException("Invalid credentials");
        }

        auditLogService.logClientAction(
                client,
                ActionType.LOGIN,
                "Successful login: " + email,
                "SYSTEM"
        );

        return client;
    }

    // =========================
    // BASIC CRUD METHODS
    // =========================
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
    }

    public Client getClientByEmail(String email) {
        return clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found with email: " + email));
    }

    public Client updateClient(Long id, Client updatedClient) {
        Client existing = getClientById(id);

        existing.setFullName(updatedClient.getFullName());
        existing.setEmail(updatedClient.getEmail());
        existing.setUsername(updatedClient.getUsername());
        // Do NOT update password here unless explicitly needed

        Client saved = clientRepository.save(existing);

        auditLogService.logClientAction(
                saved,
                ActionType.USER_UPDATED,
                "Client updated: " + saved.getEmail(),
                "SYSTEM"
        );

        return saved;
    }

    public void deleteClient(Long id) {
        Client client = getClientById(id);

        clientRepository.deleteById(id);

        auditLogService.logClientAction(
                client,
                ActionType.USER_DELETED,
                "Client deleted: " + client.getEmail(),
                "SYSTEM"
        );
    }

    public Client findByUsername(String username) {
        return clientRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}