package za.ac.alis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.alis.dto.LawRuleRequest;
import za.ac.alis.dto.LawRuleResponse;
import za.ac.alis.service.LawRuleService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class LawRuleController {

    private final LawRuleService lawRuleService;

    public LawRuleController(LawRuleService lawRuleService) {
        this.lawRuleService = lawRuleService;
    }

    @GetMapping
    public ResponseEntity<List<LawRuleResponse>> getAllRules() {
        return ResponseEntity.ok(lawRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LawRuleResponse> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(lawRuleService.getRuleById(id));
    }

    @PostMapping
    public ResponseEntity<LawRuleResponse> createRule(@RequestBody LawRuleRequest request) {
        LawRuleResponse created = lawRuleService.createRule(request);
        return ResponseEntity.created(URI.create("/api/rules/" + created.getRuleId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LawRuleResponse> updateRule(@PathVariable Long id,
                                                      @RequestBody LawRuleRequest request) {
        return ResponseEntity.ok(lawRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        lawRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}