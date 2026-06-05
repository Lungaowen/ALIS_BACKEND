package za.ac.alis.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import za.ac.alis.core.dto.LegalPractitionerDTO;
import za.ac.alis.core.persistence.LegalPractitioner;
import za.ac.alis.user.persistence.LegalPractitionerRepository;
import za.ac.alis.user.service.ClientService;

@RestController
@RequestMapping("/api/legal-practitioners")

public class LegalPractitionerController {

    private final LegalPractitionerRepository legalPractitionerRepository;
    private final ClientService               clientService;

    public LegalPractitionerController(LegalPractitionerRepository legalPractitionerRepository,
                                       ClientService clientService) {
        this.legalPractitionerRepository = legalPractitionerRepository;
        this.clientService               = clientService;
    }

    // GET /api/legal-practitioners
    @GetMapping
    public ResponseEntity<List<LegalPractitionerDTO>> getAll() {
        List<LegalPractitionerDTO> result = legalPractitionerRepository.findAllLegalPractitioners()
                .stream()
                .map(LegalPractitionerDTO::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    // GET /api/legal-practitioners/{id}
    @GetMapping("/{id}")
    public ResponseEntity<LegalPractitionerDTO> getById(@PathVariable Long id) {
        return legalPractitionerRepository.findLegalPractitionerById(id)
                .map(LegalPractitionerDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Legal Practitioner not found with id: " + id));
    }

    // POST /api/legal-practitioners
    // Body: { "fullName": "...", "email": "...", "password": "...",
    //         "barNumber": "...", "lawFirm": "..." }
    @PostMapping
    public ResponseEntity<LegalPractitionerDTO> create(@RequestBody LegalPractitionerRequest request) {
        LegalPractitioner saved = clientService.registerLegalPractitioner(
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                request.getBarNumber(),
                request.getLawFirm()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(legalPractitionerRepository.findLegalPractitionerById(saved.getClientId())
                        .map(LegalPractitionerDTO::from)
                        .orElseThrow());
    }

    // ── Typed request body ────────────────────────────────────────────────────
    public static class LegalPractitionerRequest {
        private String fullName;
        private String email;
        private String password;
        private String barNumber;
        private String lawFirm;

        public String getFullName()  { return fullName; }
        public void   setFullName(String v){ this.fullName = v; }
        public String getEmail()     { return email; }
        public void   setEmail(String v){ this.email = v; }
        public String getPassword()  { return password; }
        public void   setPassword(String v){ this.password = v; }
        public String getBarNumber() { return barNumber; }
        public void   setBarNumber(String v){ this.barNumber = v; }
        public String getLawFirm()   { return lawFirm; }
        public void   setLawFirm(String v){ this.lawFirm = v; }
    }
}
