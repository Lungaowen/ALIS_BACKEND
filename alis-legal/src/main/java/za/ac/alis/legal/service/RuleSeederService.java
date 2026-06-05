package za.ac.alis.legal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.alis.core.persistence.Act;
import za.ac.alis.core.persistence.LawRul;
import za.ac.alis.ai.service.RuleExtractionService;
import za.ac.alis.legal.persistence.LawRuleRepository;
import java.util.List;

@Service
public class RuleSeederService {

    private final RuleExtractionService extractionService;
    private final LawRuleRepository lawRuleRepository;

    public RuleSeederService(RuleExtractionService extractionService,
                             LawRuleRepository lawRuleRepository) {
        this.extractionService = extractionService;
        this.lawRuleRepository = lawRuleRepository;
    }

    /**
     * For a given Act and its raw text, extracts rules (Chapters 2‑4 only)
     * and saves them to the database. Returns the number of rules saved.
     */
    @Transactional
    public int seedRulesForAct(Act act, String actText) {
        List<LawRul> rules = extractionService.extractRules(actText, act);
        if (rules.isEmpty()) {
            return 0;
        }
        lawRuleRepository.saveAll(rules);
        return rules.size();
    }
}