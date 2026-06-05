package za.ac.alis.ai.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.LawRul;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.core.enums.AnalysisStatus;
import za.ac.alis.core.enums.RiskLevel;

/**
 * Groq-powered AI analysis service.
 *
 * Design (1 document → 1 SummaryReport):
 *  1. Pre-filter law rules using keyword overlap → pick top {@value #TOP_RULES_FOR_PROMPT} rules.
 *  2. Send ONE Groq request containing the document text + all selected rule requirements.
 *  3. Parse the JSON response into a single {@link SummaryReport} entity (unsaved).
 *  4. Link the report to the highest-scoring law rule.
 */
@Service
@ConditionalOnProperty(name = "alis.ai.groq.key")
public class AIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisService.class);

    private static final int    TOP_RULES_FOR_PROMPT = 5;
    private static final int    MAX_DOC_CHARS        = 4_000; // token budget

    private final String       groqApiKey;
    private final String       groqModel;
    private final String       groqUrl;
    private final HttpClient   http;
    private final ObjectMapper mapper;

    public AIAnalysisService(@Value("${alis.ai.groq.key:}") String groqApiKey,
                             @Value("${alis.ai.groq.model:llama-3.3-70b-versatile}") String groqModel,
                             @Value("${alis.ai.groq.url:https://api.groq.com/openai/v1/chat/completions}") String groqUrl) {
        this.groqApiKey = groqApiKey;
        this.groqModel  = groqModel;
        this.groqUrl    = groqUrl;
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Runs one Groq compliance analysis for {@code document} against all
     * provided {@code rules}.  Returns an <b>unsaved</b> {@link SummaryReport}.
     *
     * @param document      the document being analysed (client must be loaded)
     * @param extractedText plain text extracted from the document
     * @param rules         all law rules to check against
     * @return a populated (unsaved) SummaryReport
     */
    public SummaryReport analyzeDocument(Document document,
                                         String extractedText,
                                         List<LawRul> rules) {

        SummaryReport report = new SummaryReport();
        report.setDocument(document);
        report.setClient(document.getClient());
        report.setGeneratedAt(LocalDateTime.now());
        report.setModelVersion(groqModel);

        // ── Guard: no API key ──────────────────────────────────────────────────
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.error("Groq API key not configured — analysis skipped for Document ID={}",
                    document.getDocumentId());
            return fallbackReport(report, rules,
                    "AI analysis skipped: GROQ_API_KEY not set in environment.");
        }

        // ── Guard: no rules ────────────────────────────────────────────────────
        if (rules == null || rules.isEmpty()) {
            log.warn("No law rules in database — analysis skipped for Document ID={}",
                    document.getDocumentId());
            return fallbackReport(report, null,
                    "AI analysis skipped: no law rules seeded in the database.");
        }

        // ── Step 1: Pick most relevant rules ───────────────────────────────────
        List<LawRul> topRules = selectTopRules(extractedText, rules);
        LawRul primaryRule    = topRules.get(0); // highest keyword-overlap rule

        report.setLawRule(primaryRule);

        // ── Step 2: Call Groq ──────────────────────────────────────────────────
        try {
            String prompt       = buildPrompt(extractedText, topRules);
            String responseBody = callGroq(prompt);
            parseAndPopulate(responseBody, report);
            report.setAnalysisStatus(AnalysisStatus.COMPLETED);
            log.info("✅ Groq analysis complete for Document ID={} | risk={}",
                    document.getDocumentId(), report.getRiskLevel());
        } catch (Exception e) {
            log.error("❌ Groq call failed for Document ID={}: {}",
                    document.getDocumentId(), e.getMessage(), e);
            report.setAnalysisStatus(AnalysisStatus.FAILED);
            report.setRiskLevel(RiskLevel.MEDIUM);
            report.setAiExplanation("AI analysis failed: " + e.getMessage());
            report.setAiRecommendation("Please re-trigger the analysis once connectivity is restored.");
            report.setSimilarityScore(BigDecimal.ZERO);
        }

        return report;
    }

    // ── Rule pre-selection ─────────────────────────────────────────────────────

    /**
     * Scores each rule by keyword overlap with the document text and returns
     * the top {@value #TOP_RULES_FOR_PROMPT} (or all if fewer exist).
     * Always returns at least 1 rule.
     */
    private List<LawRul> selectTopRules(String text, List<LawRul> rules) {
        String lower = text.toLowerCase();

        record Scored(LawRul rule, double score) {}

        List<Scored> scored = rules.stream()
                .map(r -> new Scored(r, keywordScore(lower, r)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(TOP_RULES_FOR_PROMPT)
                .toList();

        // Ensure at least the first rule is always included
        if (scored.isEmpty()) return List.of(rules.get(0));

        return scored.stream().map(Scored::rule).collect(Collectors.toList());
    }

    private double keywordScore(String docLower, LawRul rule) {
        Set<String> tokens = new HashSet<>();
        if (rule.getKeyword() != null)
            Arrays.stream(rule.getKeyword().toLowerCase().split("\\s+"))
                  .filter(t -> t.length() > 3).forEach(tokens::add);
        if (rule.getRequirements() != null) {
            String[] words = rule.getRequirements().toLowerCase().split("\\s+");
            for (int i = 0; i < Math.min(15, words.length); i++) {
                String w = words[i].replaceAll("[^a-z]", "");
                if (w.length() > 3) tokens.add(w);
            }
        }
        if (tokens.isEmpty()) return 0.0;
        long hits = tokens.stream().filter(docLower::contains).count();
        return (double) hits / tokens.size();
    }

    // ── Prompt construction ────────────────────────────────────────────────────

    private String buildPrompt(String extractedText, List<LawRul> topRules) {
        String docSnippet = extractedText.length() > MAX_DOC_CHARS
                ? extractedText.substring(0, MAX_DOC_CHARS) + "\n[...document truncated...]"
                : extractedText;

        StringBuilder rulesBlock = new StringBuilder();
        for (int i = 0; i < topRules.size(); i++) {
            LawRul r = topRules.get(i);
            rulesBlock.append(String.format("""
                    Rule %d (%s — %s):
                    Keyword: %s
                    Requirement: %s
                    %n""",
                    i + 1,
                    r.getAct() != null ? r.getAct().getActName() : "Unknown Act",
                    r.getRiskLevel(),
                    r.getKeyword(),
                    r.getRequirements() != null ? r.getRequirements() : "N/A"));
        }

        return """
                You are a South African legal compliance expert specialising in POPIA and the Consumer Protection Act.

                Analyse the document text below against the following legal rules and determine the overall compliance risk.

                ========== LEGAL RULES ==========
                %s
                ========== END RULES ==========

                ========== DOCUMENT TEXT ==========
                %s
                ========== END DOCUMENT ==========

                Based on this analysis, return ONLY a valid JSON object (no markdown, no code fences) with exactly these keys:
                  "riskLevel"        — "HIGH", "MEDIUM", or "LOW"
                  "similarityScore"  — a number 0.0 to 100.0 indicating how closely the document relates to the rules
                  "aiExplanation"    — 3-5 sentences explaining what compliance issues were found and why the risk level was assigned
                  "aiRecommendation" — 3-5 concrete action steps to fix or maintain compliance
                """.formatted(rulesBlock.toString(), docSnippet);
    }

    // ── Groq HTTP call ─────────────────────────────────────────────────────────

    private String callGroq(String prompt) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", groqModel);
        body.put("temperature", 0.2);
        body.put("max_tokens", 1024);
        body.putObject("response_format").put("type", "json_object");

        ArrayNode messages  = body.putArray("messages");
        ObjectNode userMsg  = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(groqUrl))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Groq HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    // ── Response parsing ───────────────────────────────────────────────────────

    private void parseAndPopulate(String rawResponse, SummaryReport report) throws Exception {
        JsonNode root    = mapper.readTree(rawResponse);
        String   content = root.path("choices").path(0)
                               .path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Groq returned an empty completion");
        }

        // Strip optional markdown code fences
        content = content.replaceAll("(?s)^```json\\s*", "")
                         .replaceAll("(?s)```\\s*$", "")
                         .trim();

        JsonNode ai = mapper.readTree(content);

        // Risk level
        String riskStr = ai.path("riskLevel").asText("MEDIUM").toUpperCase().trim();
        try {
            report.setRiskLevel(RiskLevel.valueOf(riskStr));
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognised riskLevel '{}' — defaulting to MEDIUM", riskStr);
            report.setRiskLevel(RiskLevel.MEDIUM);
        }

        // Similarity score (clamped 0–100)
        double raw = ai.path("similarityScore").asDouble(0.0);
        report.setSimilarityScore(BigDecimal.valueOf(Math.max(0.0, Math.min(100.0, raw))));

        // Text fields
        report.setAiExplanation(
                ai.path("aiExplanation").asText("No explanation provided."));
        report.setAiRecommendation(
                ai.path("aiRecommendation").asText("No recommendation provided."));
    }

    // ── Fallback ───────────────────────────────────────────────────────────────

    private SummaryReport fallbackReport(SummaryReport report,
                                          List<LawRul> rules,
                                          String message) {
        if (rules != null && !rules.isEmpty()) {
            report.setLawRule(rules.get(0));
        }
        report.setAnalysisStatus(AnalysisStatus.FAILED);
        report.setRiskLevel(RiskLevel.MEDIUM);
        report.setSimilarityScore(BigDecimal.ZERO);
        report.setAiExplanation(message);
        report.setAiRecommendation("Resolve the configuration issue and re-trigger analysis.");
        return report;
    }
}
