package za.ac.alis.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import za.ac.alis.auth.security.JwtUtil;
import za.ac.alis.core.dto.AuthResponse;
import za.ac.alis.core.dto.LoginRequest;
import za.ac.alis.core.dto.RegisterRequest;
import za.ac.alis.core.enums.Role;
import za.ac.alis.core.persistence.Admin;
import za.ac.alis.core.persistence.Client;
import za.ac.alis.user.api.UserRegistrationService;
import za.ac.alis.user.persistence.AdminRepository;
import za.ac.alis.user.persistence.ClientRepository;

@Service
public class AuthService {

    private final UserRegistrationService userRegistrationService;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRegistrationService userRegistrationService,
                       ClientRepository clientRepository,
                       AdminRepository adminRepository,
                       JwtUtil jwtUtil,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRegistrationService = userRegistrationService;
        this.clientRepository = clientRepository;
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        try {
            Client saved = registerByRole(request);
            AuthResponse response = AuthResponse.success(
                    saved.getClientId(), saved.getEmail(),
                    saved.getFullName(), saved.getRole().name(),
                    "Registration successful"
            );
            response.setToken(jwtUtil.generateToken(
                    saved.getClientId().toString(),
                    saved.getRole().name()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.failure("Registration failed: " + e.getMessage()));
        }
    }

    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        String email = request.getEmail();
        String rawPassword = request.getPassword();

        var clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            if (!client.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(AuthResponse.failure("Account is deactivated"));
            }
            if (passwordEncoder.matches(rawPassword, client.getPasswordHash())) {
                AuthResponse response = AuthResponse.success(
                        client.getClientId(), client.getEmail(),
                        client.getFullName(), client.getRole().name(),
                        "Login successful"
                );
                response.setToken(jwtUtil.generateToken(
                        client.getClientId().toString(),
                        client.getRole().name()
                ));
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.failure("Invalid credentials"));
        }

        var adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
                AuthResponse response = AuthResponse.success(
                        admin.getAdminId(), admin.getEmail(),
                        admin.getFullName(), "ADMIN",
                        "Admin login successful"
                );
                response.setToken(jwtUtil.generateToken(
                        admin.getAdminId().toString(),
                        "ADMIN"
                ));
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.failure("Invalid credentials"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.failure("Invalid credentials"));
    }

    private Client registerByRole(RegisterRequest request) {
        Role requestedRole = request.getRole() == null ? Role.USER : request.getRole();
        return switch (requestedRole) {
            case USER -> userRegistrationService.registerClient(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword()
            );
            case DEAL_MAKER -> userRegistrationService.registerDealMaker(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword(),
                    requiredField(request.getCompanyName(), "companyName"),
                    requiredField(request.getDealSpecialty(), "dealSpecialty")
            );
            case LEGAL_PRACTITIONER -> userRegistrationService.registerLegalPractitioner(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword(),
                    requiredField(request.getBarNumber(), "barNumber"),
                    requiredField(request.getLawFirm(), "lawFirm")
            );
            default -> throw new IllegalArgumentException("Unsupported registration role: " + requestedRole);
        };
    }

    private String requiredField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
