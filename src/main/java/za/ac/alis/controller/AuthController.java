package za.ac.alis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.alis.dto.AuthResponse;
import za.ac.alis.dto.LoginRequest;
import za.ac.alis.dto.RegisterRequest;
import za.ac.alis.entities.Client;
import za.ac.alis.service.ClientService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ClientService clientService;

    public AuthController(ClientService clientService) {
        this.clientService = clientService;
    }

    // POST /api/auth/register
    // Body: { "fullName": "...", "email": "...", "password": "..." }
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            Client saved = clientService.registerClient(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword()
            );
            return ResponseEntity.ok(AuthResponse.success(
                    saved.getClientId(),
                    saved.getEmail(),
                    saved.getFullName(),
                    saved.getRole().name(),
                    "Registration successful"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.failure("Registration failed: " + e.getMessage()));
        }
    }

    // POST /api/auth/login
    // Body: { "email": "...", "password": "..." }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            Client client = clientService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(AuthResponse.success(
                    client.getClientId(),
                    client.getEmail(),
                    client.getFullName(),
                    client.getRole().name(),
                    "Login successful"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401)
                    .body(AuthResponse.failure(e.getMessage()));
        }
    }
}
