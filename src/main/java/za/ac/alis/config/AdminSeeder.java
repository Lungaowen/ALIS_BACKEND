package za.ac.alis.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import za.ac.alis.entities.Admin;
import za.ac.alis.repo.AdminRepository;

@Component
@Profile("!test")
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminRepository adminRepository;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminSeeder(AdminRepository adminRepository,
                       @Value("${alis.seed.admin.name:System Admin}") String adminName,
                       @Value("${alis.seed.admin.email:}") String adminEmail,
                       @Value("${alis.seed.admin.password:}") String adminPassword) {
        this.adminRepository = adminRepository;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("Admin seeding skipped because bootstrap credentials are not configured");
            return;
        }

        if (adminRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        Admin admin = new Admin();
        admin.setFullName(adminName);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setCreatedAt(LocalDateTime.now());
        adminRepository.save(admin);
        log.info("Bootstrap admin account created for {}", adminEmail);
    }
}
