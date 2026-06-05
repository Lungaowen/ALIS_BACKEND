package za.ac.alis.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.ac.alis.core.dto.ClientProfileResponse;
import za.ac.alis.core.dto.ClientProfileUpdateRequest;
import za.ac.alis.core.persistence.Client;
import za.ac.alis.user.service.ClientService;

@RestController
@RequestMapping("/api/client/profile")
@PreAuthorize("hasAnyRole('USER','LEGAL_PRACTITIONER','DEAL_MAKER')")
public class ClientProfileController {

    private final ClientService clientService;

    public ClientProfileController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<ClientProfileResponse> getProfile() {
        Long clientId = getAuthenticatedClientId();
        Client client = clientService.getOwnProfile(clientId);
        return ResponseEntity.ok(ClientProfileResponse.from(client));
    }

    @PutMapping
    public ResponseEntity<ClientProfileResponse> updateProfile(@RequestBody ClientProfileUpdateRequest request) {
        Long clientId = getAuthenticatedClientId();
        Client updatedClient = clientService.updateOwnProfile(clientId, request);
        return ResponseEntity.ok(ClientProfileResponse.from(updatedClient));
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ClientProfileResponse> deactivateProfile() {
        Long clientId = getAuthenticatedClientId();
        Client deactivatedClient = clientService.deactivateOwnAccount(clientId);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ClientProfileResponse.from(deactivatedClient));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteProfile() {
        Long clientId = getAuthenticatedClientId();
        clientService.deleteOwnAccount(clientId);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", "Account deleted successfully"));
    }

    private Long getAuthenticatedClientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return Long.valueOf(principal);
        }
        throw new IllegalStateException("Not authenticated");
    }
}
