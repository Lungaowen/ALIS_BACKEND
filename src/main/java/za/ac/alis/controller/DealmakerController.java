package za.ac.alis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.ac.alis.dto.DealMakerDTO;
import za.ac.alis.dto.RegisterRequest;
import za.ac.alis.entities.DealMaker;
import za.ac.alis.repo.DealMakerRepository;
import za.ac.alis.service.ClientService;

import java.util.List;

@RestController
@RequestMapping("/api/dealmakers")
public class DealMakerController {

    private final DealMakerRepository dealMakerRepository;
    private final ClientService       clientService;

    public DealMakerController(DealMakerRepository dealMakerRepository,
                                ClientService clientService) {
        this.dealMakerRepository = dealMakerRepository;
        this.clientService       = clientService;
    }

    // GET /api/dealmakers
    @GetMapping
    public ResponseEntity<List<DealMakerDTO>> getAll() {
        List<DealMakerDTO> result = dealMakerRepository.findAllDealMakers()
                .stream()
                .map(DealMakerDTO::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    // GET /api/dealmakers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DealMakerDTO> getById(@PathVariable Long id) {
        return dealMakerRepository.findDealMakerById(id)
                .map(DealMakerDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DealMaker not found with id: " + id));
    }

    // POST /api/dealmakers
    // Body: { "fullName": "...", "email": "...", "password": "...",
    //         "companyName": "...", "dealSpecialty": "..." }
    @PostMapping
    public ResponseEntity<DealMakerDTO> create(@RequestBody DealMakerRequest request) {
        DealMaker saved = clientService.registerDealMaker(
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                request.getCompanyName(),
                request.getDealSpecialty()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dealMakerRepository.findDealMakerById(saved.getClientId())
                        .map(DealMakerDTO::from)
                        .orElseThrow());
    }

    // ── Typed request body (no raw entity as @RequestBody) ───────────────────
    public static class DealMakerRequest {
        private String fullName;
        private String email;
        private String password;
        private String companyName;
        private String dealSpecialty;

        public String getFullName()      { return fullName; }
        public void   setFullName(String v){ this.fullName = v; }
        public String getEmail()         { return email; }
        public void   setEmail(String v) { this.email = v; }
        public String getPassword()      { return password; }
        public void   setPassword(String v){ this.password = v; }
        public String getCompanyName()   { return companyName; }
        public void   setCompanyName(String v){ this.companyName = v; }
        public String getDealSpecialty() { return dealSpecialty; }
        public void   setDealSpecialty(String v){ this.dealSpecialty = v; }
    }
}
