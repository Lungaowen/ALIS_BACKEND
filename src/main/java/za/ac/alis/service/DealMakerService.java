package za.ac.alis.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.entities.DealMaker;
import za.ac.alis.enums.Role;
import za.ac.alis.repo.ClientRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DealMakerService {

    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DealMakerService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    // =========================================================
    // REGISTER DEAL MAKER
    // =========================================================
    @Transactional
    public DealMaker registerDealMaker(
            String fullName,
            String email,
            String password,
            String companyName
    ) {

        if (clientRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        DealMaker dm = new DealMaker();
        dm.setFullName(fullName);
        dm.setEmail(email);

        // 🔐 password hashing
        dm.setPasswordHash(encoder.encode(password));

        // role assignment (VERY IMPORTANT for polymorphism)
        dm.setRole(Role.DEAL_MAKER);

        dm.setCompanyName(companyName);

        dm.setUsername(generateUniqueUsername(fullName));
        dm.setCreatedAt(LocalDateTime.now());

        return clientRepository.save(dm);
    }

    // =========================================================
    // GET ALL DEAL MAKERS
    // =========================================================
    public List<DealMaker> getAllDealMakers() {
        return clientRepository.findAll()
                .stream()
                .filter(c -> c instanceof DealMaker)
                .map(c -> (DealMaker) c)
                .toList();
    }

    // =========================================================
    // GET BY EMAIL
    // =========================================================
    public DealMaker getByEmail(String email) {
        return (DealMaker) clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("DealMaker not found"));
    }

    // =========================================================
    // UPDATE DEAL MAKER
    // =========================================================
    @Transactional
    public DealMaker updateDealMaker(Long id, DealMaker updated) {

        DealMaker existing = (DealMaker) clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DealMaker not found"));

        existing.setFullName(updated.getFullName());
        existing.setEmail(updated.getEmail());
        existing.setCompanyName(updated.getCompanyName());

        return clientRepository.save(existing);
    }

    // =========================================================
    // DELETE DEAL MAKER
    // =========================================================
    public void deleteDealMaker(Long id) {
        clientRepository.deleteById(id);
    }

    // =========================================================
    // USERNAME GENERATOR (same logic as ClientService)
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