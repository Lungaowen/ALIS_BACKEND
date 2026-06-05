package za.ac.alis.legal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.core.persistence.Clause;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.LawRul;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.core.enums.AnalysisStatus;
import za.ac.alis.core.enums.RiskLevel;
import za.ac.alis.legal.persistence.ClauseRepository;
import za.ac.alis.legal.persistence.LawRuleRepository;
import za.ac.alis.legal.persistence.SummaryReportRepository;

/**
 * Core compliance engine.
 *
 * For each clause extracted from the document, this service:
 *   1. Loads all {@link LawRul} records from the database.
 *   2. Scores each (clause, rule) pair using keyword overlap.
 *   3. Creates a {@link SummaryReport} for any pair that exceeds the
 *      similarity threshold (default 20 %).
 *   4. Updates the clause risk level to match the highest matched rule.
 */
@Service
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    /** Minimum keyword-overlap % to create a SummaryReport. */
    private static final double SIMILARITY_THRESHOLD = 0.20;

    /** Model version tag written to every report row. */
    private static final String MODEL_VERSION = "alis-keyword-v1.0";

    private final LawRuleRepository      lawRuleRepository;
    private final SummaryReportRepository summaryReportRepository;
    private final ClauseRepository        clauseRepository;

    public ComplianceService(LawRuleRepository lawRuleRepository,
                             SummaryReportRepository summaryReportRepository,
                             ClauseRepository clauseRepository) {
        this.lawRuleRepository      = lawRuleRepository;
        this.summaryReportRepository = summaryReportRepository;
        this.clauseRepository        = clauseRepository;
    }

    /**
     * Runs the compliance check for a document.
     *
     * @param document the analyzed document (must have its Client loaded)
     * @param clauses  list of extracted {@link Clause} entities (already persisted)
     * @return list of persisted {@link SummaryReport} records
     */
    @Transactional
    public List<SummaryReport> runCompliance(Document document, List<Clause> clauses) {

        List<LawRul> rules   = lawRuleRepository.findAllWithAct();
        List<SummaryReport> reports = new ArrayList<>();

        if (rules.isEmpty()) {
            log.warn("No law rules found — skipping compliance check for Document ID={}",
                    document.getDocumentId());
            return reports;
        }

        log.info("Running compliance check: {} clauses × {} rules for Document ID={}",
                clauses.size(), rules.size(), document.getDocumentId());

        for (Clause clause : clauses) {
            String clauseLower = clause.getClauseText().toLowerCase();

            for (LawRul rule : rules) {
                double score = computeSimilarity(clauseLower, rule);
                if (score < SIMILARITY_THRESHOLD) continue;

                RiskLevel risk = resolveRisk(clauseLower, rule.getRiskLevel());

                // Upgrade clause risk level if this match is higher
                if (risk.ordinal() > clause.getRiskLevel().ordinal()) {
                    clause.setRiskLevel(risk);
                    clause.setRiskReason("Matched rule: " + rule.getKeyword()
                            + " (" + rule.getAct().getActName() + ")");
                }

                SummaryReport report = new SummaryReport();
                report.setDocument(document);
                report.setClient(document.getClient());
                report.setLawRule(rule);
                report.setSimilarityScore(
                        BigDecimal.valueOf(score * 100).setScale(2, RoundingMode.HALF_UP));
                report.setRiskLevel(risk);
                report.setAnalysisStatus(AnalysisStatus.COMPLETED);
                report.setGeneratedAt(LocalDateTime.now());
                report.setModelVersion(MODEL_VERSION);
                report.setAiRecommendation(buildRecommendation(clause, rule));
                report.setAiExplanation(buildExplanation(clause, rule, score));

                reports.add(summaryReportRepository.save(report));
            }

            // Persist updated clause risk
            clauseRepository.save(clause);
        }

        log.info("Compliance check complete: {} reports generated for Document ID={}",
                reports.size(), document.getDocumentId());
        return reports;
    }

    // ── Similarity ────────────────────────────────────────────────────────────

    /**
     * Keyword-overlap similarity between a clause and a law rule.
     *
     * Score = (number of rule keywords found in clause) / (total rule keywords)
     *
     * Rule keywords are derived from:
     *   - the rule's keyword field (split by space)
     *   - the first 12 words of the requirements field
     */
    private double computeSimilarity(String clauseLower, LawRul rule) {
        Set<String> ruleTokens = extractTokens(rule);
        if (ruleTokens.isEmpty()) return 0.0;

        long hits = ruleTokens.stream()
                .filter(token -> clauseLower.contains(token))
                .count();

        return (double) hits / ruleTokens.size();
    }

    private Set<String> extractTokens(LawRul rule) {
        List<String> tokens = new ArrayList<>();

        // keyword field
        if (rule.getKeyword() != null) {
            tokens.addAll(Arrays.asList(rule.getKeyword().toLowerCase().split("\\s+")));
        }

        // first 12 significant words from requirements
        if (rule.getRequirements() != null) {
            String[] words = rule.getRequirements().toLowerCase().split("\\s+");
            for (int i = 0; i < Math.min(12, words.length); i++) {
                String w = words[i].replaceAll("[^a-z]", "");
                if (w.length() > 3) tokens.add(w); // skip short stop-words
            }
        }

        return tokens.stream()
                .filter(t -> t.length() > 3)
                .collect(Collectors.toSet());
    }

    // ── Risk resolution ───────────────────────────────────────────────────────

    /**
     * Upgrades risk to HIGH if the clause text contains prohibition/penalty language,
     * otherwise uses the rule's defined risk level.
     */
    private RiskLevel resolveRisk(String clauseLower, RiskLevel baseRisk) {
        boolean hasProhibition = clauseLower.matches(
                ".*\\b(prohibit|offence|penalty|void|must not|shall not|may not|"
                        + "unlawful|fine|imprisonment|liable to|prohibited)\\b.*");
        if (hasProhibition) return RiskLevel.HIGH;

        boolean hasMandatory = clauseLower.matches(
                ".*\\b(must|shall|required|mandatory|obliged|duty to)\\b.*");
        if (hasMandatory && baseRisk == RiskLevel.LOW) return RiskLevel.MEDIUM;

        return baseRisk;
    }

    // ── Report text generation ─────────────────────────────────────────────────

    private String buildRecommendation(Clause clause, LawRul rule) {
        if (rule.getSuggestion() != null && !rule.getSuggestion().isBlank()) {
            return rule.getSuggestion();
        }
        return switch (rule.getRiskLevel()) {
            case HIGH   -> "Immediate legal review required. This clause may violate "
                    + rule.getAct().getActName() + " regarding: " + rule.getKeyword();
            case MEDIUM -> "Review and amend this clause to comply with "
                    + rule.getAct().getActName() + " regarding: " + rule.getKeyword();
            case LOW    -> "Minor review recommended for alignment with "
                    + rule.getAct().getActName() + ".";
        };
    }

    private String buildExplanation(Clause clause, LawRul rule, double score) {
        String pct = String.format("%.0f%%", score * 100);
        return String.format(
                "Clause matched rule '%s' (Act: %s) with a similarity score of %s.\n\n"
                        + "Matched clause:\n\"%s\"\n\n"
                        + "Rule requirement:\n\"%s\"",
                rule.getKeyword(),
                rule.getAct().getActName(),
                pct,
                truncate(clause.getClauseText(), 400),
                truncate(rule.getRequirements(), 400)
        );
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}