package za.ac.alis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import za.ac.alis.dto.AuthResponse;
import za.ac.alis.dto.LoginRequest;
import za.ac.alis.dto.RegisterRequest;
import za.ac.alis.entities.Admin;
import za.ac.alis.entities.Client;
import za.ac.alis.repo.AdminRepository;
import za.ac.alis.repo.ClientRepository;
import za.ac.alis.security.JwtUtil;
import za.ac.alis.service.ClientService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ClientService clientService;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(ClientService clientService,
                          ClientRepository clientRepository,
                          AdminRepository adminRepository,
                          JwtUtil jwtUtil) {
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            Client saved = clientService.registerClient(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword()
            );
            String token = jwtUtil.generateToken(
                    saved.getClientId().toString(),
                    saved.getRole().name()
            );
            AuthResponse response = AuthResponse.success(
                    saved.getClientId(), saved.getEmail(),
                    saved.getFullName(), saved.getRole().name(),
                    "Registration successful"
            );
            response.setToken(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.failure("Registration failed: " + e.getMessage()));
        }
    }

    // POST /api/auth/login – handles both clients and admins
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        String email = request.getEmail();
        String rawPassword = request.getPassword();

        // 1. Try client login
        var clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            if (passwordEncoder.matches(rawPassword, client.getPasswordHash())) {
                String token = jwtUtil.generateToken(
                        client.getClientId().toString(),
                        client.getRole().name()
                );
                AuthResponse response = AuthResponse.success(
                        client.getClientId(), client.getEmail(),
                        client.getFullName(), client.getRole().name(),
                        "Login successful"
                );
                response.setToken(token);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401)
                        .body(AuthResponse.failure("Invalid credentials"));
            }
        }

        // 2. Try admin login
        var adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
                String token = jwtUtil.generateToken(
                        admin.getAdminId().toString(),
                        "ADMIN"
                );
                AuthResponse response = AuthResponse.success(
                        admin.getAdminId(), admin.getEmail(),
                        admin.getFullName(), "ADMIN",
                        "Admin login successful"
                );
                response.setToken(token);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401)
                        .body(AuthResponse.failure("Invalid credentials"));
            }
        }

        return ResponseEntity.status(401)
                .body(AuthResponse.failure("Invalid credentials"));
    }
}