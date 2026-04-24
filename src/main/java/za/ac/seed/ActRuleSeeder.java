package za.ac.seed;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import za.ac.alis.entities.Act;
import za.ac.alis.repo.ActRepository;
import za.ac.alis.service.RuleSeederService;
import za.ac.alis.service.TextExtractionService;

@Component
@ConditionalOnProperty(name = "rule.seeding.enabled", havingValue = "true", matchIfMissing = false)
public class ActRuleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ActRuleSeeder.class);

    private final ActRepository actRepository;
    private final RuleSeederService ruleSeederService;
    private final TextExtractionService textExtractionService;

    public ActRuleSeeder(ActRepository actRepository,
                         RuleSeederService ruleSeederService,
                         TextExtractionService textExtractionService) {
        this.actRepository = actRepository;
        this.ruleSeederService = ruleSeederService;
        this.textExtractionService = textExtractionService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Skip if rules already exist for this act
        if (actRepository.findByActName("Consumer Protection Act").isPresent()) {
            log.info("Act already seeded – skipping.");
            return;
        }

        // 1. Load PDF from classpath
        ClassPathResource resource = new ClassPathResource("CPA_Act.pdf");
        if (!resource.exists()) {
            log.error("PDF not found at src/main/resources/acts/CPA_Act.pdf");
            return;
        }

        byte[] pdfBytes = Files.readAllBytes(Path.of(resource.getURI()));

        // 2. Extract text using existing service
      String extractedText = textExtractionService.extractTextFromPdfBytes(pdfBytes);

        // 3. Create the Act entity
        Act act = new Act();
        act.setActName("Consumer Protection Act");
        act.setActNumber("68 of 2008");
        act.setActYear(2008);
        act = actRepository.save(act);

        // 4. Extract and save rules
        int count = ruleSeederService.seedRulesForAct(act, extractedText);
        log.info("✅ Seeded {} rules for {}", count, act.getActName());
    }
}