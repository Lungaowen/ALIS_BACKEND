package za.ac.alis.controller;

import org.springframework.web.bind.annotation.*;
import za.ac.alis.entities.LegalPractitioner;
import za.ac.alis.repo.LegalPractitionerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/legal-practitioners")
public class LegalPractitionerController {

    private final LegalPractitionerRepository repo;

    public LegalPractitionerController(LegalPractitionerRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public LegalPractitioner create(@RequestBody LegalPractitioner lp) {
        return repo.save(lp);
    }

    @GetMapping
    public List<LegalPractitioner> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public LegalPractitioner getById(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Legal Practitioner not found"));
    }
}