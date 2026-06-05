package za.ac.alis.legal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.core.dto.LawRuleRequest;
import za.ac.alis.core.dto.LawRuleResponse;
import za.ac.alis.core.persistence.Act;
import za.ac.alis.core.persistence.LawRul;
import za.ac.alis.legal.persistence.ActRepository;
import za.ac.alis.legal.persistence.LawRuleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LawRuleService {

    private final LawRuleRepository lawRuleRepository;
    private final ActRepository actRepository;

    public LawRuleService(LawRuleRepository lawRuleRepository,
                          ActRepository actRepository) {
        this.lawRuleRepository = lawRuleRepository;
        this.actRepository = actRepository;
    }

    public List<LawRuleResponse> getAllRules() {
        return lawRuleRepository.findAllWithAct().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public LawRuleResponse getRuleById(Long id) {
        LawRul rule = lawRuleRepository.findByIdWithAct(id)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + id));
        return toResponse(rule);
    }

    @Transactional
    public LawRuleResponse createRule(LawRuleRequest request) {
        Act act = actRepository.findById(request.getActId())
                .orElseThrow(() -> new RuntimeException("Act not found with id: " + request.getActId()));

        LawRul rule = new LawRul();
        rule.setAct(act);
        rule.setKeyword(request.getKeyword());
        rule.setRequirements(request.getRequirements());
        rule.setRiskLevel(request.getRiskLevel());
        rule.setSuggestion(request.getSuggestion());
        rule.setEdited(false);

        LawRul saved = lawRuleRepository.save(rule);
        return toResponse(saved);
    }

    @Transactional
    public LawRuleResponse updateRule(Long id, LawRuleRequest request) {
        LawRul rule = lawRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + id));

        if (request.getActId() != null && !request.getActId().equals(rule.getAct().getActId())) {
            Act act = actRepository.findById(request.getActId())
                    .orElseThrow(() -> new RuntimeException("Act not found"));
            rule.setAct(act);
        }
        if (request.getKeyword() != null) rule.setKeyword(request.getKeyword());
        if (request.getRequirements() != null) rule.setRequirements(request.getRequirements());
        if (request.getRiskLevel() != null) rule.setRiskLevel(request.getRiskLevel());
        if (request.getSuggestion() != null) rule.setSuggestion(request.getSuggestion());
        rule.setEdited(true);

        LawRul updated = lawRuleRepository.save(rule);
        return toResponse(updated);
    }

    @Transactional
    public void deleteRule(Long id) {
        lawRuleRepository.deleteById(id);
    }

    private LawRuleResponse toResponse(LawRul rule) {
        return new LawRuleResponse(
                rule.getRuleId(),
                rule.getAct().getActId(),
                rule.getAct().getActName(),
                rule.getKeyword(),
                rule.getRequirements(),
                rule.getRiskLevel(),
                rule.getSuggestion(),
                rule.getEdited()
        );
    }
}