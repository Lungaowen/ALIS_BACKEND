package za.ac.alis.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.entities.LegalPractitioner;
import za.ac.alis.enums.Role;
import za.ac.alis.repo.ClientRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LegalPractitionerService {

    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LegalPractitionerService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    // =========================================================
    // REGISTER LEGAL PRACTITIONER
    // =========================================================
    @Transactional
    public LegalPractitioner registerLegalPractitioner(
            String fullName,
            String email,
            String password,
            String barNumber,
            String lawFirm
    ) {

        if (clientRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        LegalPractitioner lp = new LegalPractitioner();

        lp.setFullName(fullName);
        lp.setEmail(email);

        // 🔐 Secure password hashing
        lp.setPasswordHash(encoder.encode(password));

        // ✔ IMPORTANT: role defines subtype behavior
        lp.setRole(Role.LEGAL_PRACTITIONER);

        lp.setBarNumber(barNumber);
        lp.setLawFirm(lawFirm);

        lp.setUsername(generateUniqueUsername(fullName));
        lp.setCreatedAt(LocalDateTime.now());

        return clientRepository.save(lp);
    }

    // =========================================================
    // GET ALL LEGAL PRACTITIONERS
    // =========================================================
    public List<LegalPractitioner> getAllLegalPractitioners() {
        return clientRepository.findAll()
                .stream()
                .filter(c -> c instanceof LegalPractitioner)
                .map(c -> (LegalPractitioner) c)
                .toList();
    }

    // =========================================================
    // GET BY EMAIL
    // =========================================================
    public LegalPractitioner getByEmail(String email) {
        return (LegalPractitioner) clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Legal Practitioner not found"));
    }

    // =========================================================
    // UPDATE LEGAL PRACTITIONER
    // =========================================================
    @Transactional
    public LegalPractitioner updateLegalPractitioner(Long id, LegalPractitioner updated) {

        LegalPractitioner existing = (LegalPractitioner) clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Legal Practitioner not found"));

        existing.setFullName(updated.getFullName());
        existing.setEmail(updated.getEmail());
        existing.setBarNumber(updated.getBarNumber());
        existing.setLawFirm(updated.getLawFirm());

        return clientRepository.save(existing);
    }

    // =========================================================
    // DELETE LEGAL PRACTITIONER
    // =========================================================
    public void deleteLegalPractitioner(Long id) {
        clientRepository.deleteById(id);
    }

    // =========================================================
    // USERNAME GENERATOR (shared logic style)
    // =========================================================
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