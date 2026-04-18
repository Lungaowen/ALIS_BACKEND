package za.ac.alis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.alis.entities.Client;
import za.ac.alis.service.ClientService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final ClientService clientService;

    public AuthController(ClientService clientService) {
        this.clientService = clientService;
    }

    // ====================== REGISTER ======================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Client client) {
        try {
            Client savedClient = clientService.registerClient(client);

            return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "clientId", savedClient.getClientId(),
                "email", savedClient.getEmail(),
                "role", savedClient.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Registration failed: " + e.getMessage()
            ));
        }
    }

    // ====================== LOGIN ======================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required"));
            }

            Client client = clientService.login(email, password);

            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "clientId", client.getClientId(),
                "email", client.getEmail(),
                "role", client.getRole(),
                "fullName", client.getFullName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of(
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Login failed: " + e.getMessage()
            ));
        }
    }
}