package za.ac.alis.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.dto.ClientProfileUpdateRequest;
import za.ac.alis.entities.Client;
import za.ac.alis.entities.DealMaker;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.LegalPractitioner;
import za.ac.alis.enums.ActionType;
import za.ac.alis.enums.Role;
import za.ac.alis.repo.AuditLogRepository;
import za.ac.alis.repo.ClientRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;
    private final AuditLogRepository auditLogRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ClientService(ClientRepository clientRepository,
                         AuditLogService auditLogService,
                         DocumentService documentService,
                         AuditLogRepository auditLogRepository) {
        this.clientRepository = clientRepository;
        this.auditLogService = auditLogService;
        this.documentService = documentService;
        this.auditLogRepository = auditLogRepository;
    }

    // -------------------- Registration (overloaded) --------------------
    public Client registerClient(String fullName, String email, String password) {
        if (clientRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        Client client = new Client();
        client.setFullName(fullName);
        client.setEmail(email);
        client.setPasswordHash(passwordEncoder.encode(password));
        client.setRole(Role.USER);
        client.setUsername(generateUniqueUsername(fullName));
        client.setCreatedAt(LocalDateTime.now());
        Client saved = clientRepository.save(client);
        auditLogService.logClientAction(saved, ActionType.USER_CREATED, "Client registered: " + email, "SYSTEM");
        return saved;
    }

    public DealMaker registerDealMaker(String fullName, String email, String password,
                                       String companyName, String dealSpecialty) {
        if (clientRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        DealMaker dm = new DealMaker();
        dm.setFullName(fullName);
        dm.setEmail(email);
        dm.setPasswordHash(passwordEncoder.encode(password));
        dm.setRole(Role.DEAL_MAKER);
        dm.setUsername(generateUniqueUsername(fullName));
        dm.setCreatedAt(LocalDateTime.now());
        dm.setCompanyName(companyName);
        dm.setDealSpecialty(dealSpecialty);
        DealMaker saved = clientRepository.save(dm);
        auditLogService.logClientAction(saved, ActionType.USER_CREATED, "DealMaker registered: " + email, "SYSTEM");
        return saved;
    }

    public LegalPractitioner registerLegalPractitioner(String fullName, String email, String password,
                                                       String barNumber, String lawFirm) {
        if (clientRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        LegalPractitioner lp = new LegalPractitioner();
        lp.setFullName(fullName);
        lp.setEmail(email);
        lp.setPasswordHash(passwordEncoder.encode(password));
        lp.setRole(Role.LEGAL_PRACTITIONER);
        lp.setUsername(generateUniqueUsername(fullName));
        lp.setCreatedAt(LocalDateTime.now());
        lp.setBarNumber(barNumber);
        lp.setLawFirm(lawFirm);
        LegalPractitioner saved = clientRepository.save(lp);
        auditLogService.logClientAction(saved, ActionType.USER_CREATED, "Legal Practitioner registered: " + email, "SYSTEM");
        return saved;
    }

    // -------------------- Login --------------------
    public Client login(String email, String rawPassword) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!client.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        if (!passwordEncoder.matches(rawPassword, client.getPasswordHash())) {
            auditLogService.logClientAction(client, ActionType.LOGIN, "Failed login attempt for: " + email, "SYSTEM");
            throw new RuntimeException("Invalid credentials");
        }
        auditLogService.logClientAction(client, ActionType.LOGIN, "Successful login: " + email, "SYSTEM");
        return client;
    }

    // -------------------- CRUD --------------------
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    public Client getOwnProfile(Long clientId) {
        return getClientById(clientId);
    }

    public Client updateClient(Long id, Client updated) {
        Client existing = getClientById(id);
        existing.setFullName(updated.getFullName());
        existing.setEmail(updated.getEmail());
        existing.setUsername(updated.getUsername());
        // Do not update password here unless explicitly intended
        return clientRepository.save(existing);
    }

    public Client updateOwnProfile(Long clientId, ClientProfileUpdateRequest request) {
        Client existing = getClientById(clientId);
        boolean changed = false;

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isEmpty()) {
                throw new RuntimeException("Full name cannot be blank");
            }
            existing.setFullName(fullName);
            changed = true;
        }

        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (username.isEmpty()) {
                throw new RuntimeException("Username cannot be blank");
            }
            if (!username.equals(existing.getUsername()) && clientRepository.existsByUsername(username)) {
                throw new RuntimeException("Username already exists");
            }
            existing.setUsername(username);
            changed = true;
        }

        boolean hasCurrentPassword = request.getCurrentPassword() != null && !request.getCurrentPassword().isBlank();
        boolean hasNewPassword = request.getNewPassword() != null && !request.getNewPassword().isBlank();

        if (hasCurrentPassword != hasNewPassword) {
            throw new RuntimeException("Both currentPassword and newPassword are required to change password");
        }

        if (hasCurrentPassword) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), existing.getPasswordHash())) {
                throw new RuntimeException("Current password is incorrect");
            }
            existing.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            changed = true;
        }

        if (existing instanceof LegalPractitioner legalPractitioner) {
            if (request.getBarNumber() != null) {
                legalPractitioner.setBarNumber(requiredValue(request.getBarNumber(), "barNumber"));
                changed = true;
            }
            if (request.getLawFirm() != null) {
                legalPractitioner.setLawFirm(requiredValue(request.getLawFirm(), "lawFirm"));
                changed = true;
            }
        } else if (existing instanceof DealMaker dealMaker) {
            if (request.getCompanyName() != null) {
                dealMaker.setCompanyName(requiredValue(request.getCompanyName(), "companyName"));
                changed = true;
            }
            if (request.getDealSpecialty() != null) {
                dealMaker.setDealSpecialty(requiredValue(request.getDealSpecialty(), "dealSpecialty"));
                changed = true;
            }
        }

        if (!changed) {
            return existing;
        }

        Client saved = clientRepository.save(existing);
        auditLogService.logClientAction(saved, ActionType.USER_UPDATED,
                "Client profile updated: " + saved.getEmail(), "SELF_SERVICE");
        return saved;
    }

    public Client deactivateOwnAccount(Long clientId) {
        Client existing = getClientById(clientId);
        if (!existing.isActive()) {
            return existing;
        }

        existing.setActive(false);
        existing.setDeactivatedAt(LocalDateTime.now());
        Client saved = clientRepository.save(existing);
        auditLogService.logClientAction(saved, ActionType.USER_UPDATED,
                "Client account deactivated: " + saved.getEmail(), "SELF_SERVICE");
        return saved;
    }

    public void deleteOwnAccount(Long clientId) {
        Client existing = getClientById(clientId);
        List<Document> documents = documentService.getDocumentsByClientId(clientId);
        for (Document document : documents) {
            documentService.deleteDocument(document.getDocumentId());
        }

        auditLogRepository.deleteByClientClientId(clientId);
        clientRepository.delete(existing);
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    // -------------------- Helper --------------------
    private String requiredValue(String value, String fieldName) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new RuntimeException(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    private String generateUniqueUsername(String fullName) {
        String base = fullName.toLowerCase().replaceAll("\\s+", "");
        String username;
        do {
            int random = (int) (Math.random() * 10000);
            username = base + "_" + random;
        } while (clientRepository.existsByUsername(username));
        return username;
    }
}
