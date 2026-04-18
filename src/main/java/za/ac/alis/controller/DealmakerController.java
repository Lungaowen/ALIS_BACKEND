package za.ac.alis.controller;

import org.springframework.web.bind.annotation.*;

import za.ac.alis.entities.DealMaker;

import za.ac.alis.repo.DealMakerRepository;


import java.util.List;

@RestController
@RequestMapping("/api/dealmakers")
public class DealmakerController {

    private final DealMakerRepository repo;

    public DealmakerController(DealMakerRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public DealMaker create(@RequestBody DealMaker dm) {
        return repo.save(dm);
    }

    @GetMapping
    public List<DealMaker> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public DealMaker getById(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Dealmaker not found"));
    }
}