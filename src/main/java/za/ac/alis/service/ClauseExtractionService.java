package za.ac.alis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import za.ac.alis.entities.Clause;
import za.ac.alis.entities.Document;
import za.ac.alis.enums.RiskLevel;

/**
 * Splits extracted document text into individual legal clauses.
 *
 * Strategy:
 *   1. Split by paragraph breaks.
 *   2. Further split oversized paragraphs by sentence boundaries.
 *   3. Filter out noise (headers, blank lines, single-word lines).
 *   4. Assign a provisional risk level (LOW) — the ComplianceService
 *      will upgrade the risk after rule matching.
 */
@Service
public class ClauseExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ClauseExtractionService.class);

    private static final int    MIN_CLAUSE_WORDS  = 6;
    private static final int    MAX_CLAUSE_CHARS  = 1_500;
    private static final Pattern PARAGRAPH_SPLIT  = Pattern.compile("\\r?\\n{2,}");
    private static final Pattern SENTENCE_SPLIT   = Pattern.compile("(?<=[.?!])\\s+");
    private static final Pattern HEADING_PATTERN  = Pattern.compile(
            "^(\\d+\\.\\s+|[A-Z]{3,}\\s*[:–-]|CHAPTER|PART|SECTION|SCHEDULE|ANNEXURE).*",
            Pattern.CASE_INSENSITIVE);

    /**
     * Extracts clauses from plain text and returns unsaved {@link Clause} entities
     * (the caller is responsible for persisting them).
     *
     * @param document      the parent Document entity
     * @param extractedText full plain-text of the document
     * @return list of Clause objects ready to persist
     */
    public List<Clause> extractClauses(Document document, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            log.warn("No text to extract clauses from for Document ID={}", document.getDocumentId());
            return List.of();
        }

        List<Clause> clauses = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_SPLIT.split(extractedText.trim());

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();

            if (trimmed.isBlank()) continue;
            if (HEADING_PATTERN.matcher(trimmed).matches()) continue;
            if (wordCount(trimmed) < MIN_CLAUSE_WORDS) continue;

            if (trimmed.length() <= MAX_CLAUSE_CHARS) {
                clauses.add(buildClause(document, trimmed));
            } else {
                // Oversized paragraph — split into sentences
                for (String sentence : SENTENCE_SPLIT.split(trimmed)) {
                    String s = sentence.trim();
                    if (wordCount(s) >= MIN_CLAUSE_WORDS) {
                        clauses.add(buildClause(document, s));
                    }
                }
            }
        }

        log.info("Extracted {} clauses for Document ID={}", clauses.size(), document.getDocumentId());
        return clauses;
    }

    private Clause buildClause(Document document, String text) {
        Clause c = new Clause();
        c.setDocument(document);
        c.setClauseText(text);
        c.setRiskLevel(RiskLevel.LOW); // ComplianceService will update this
        return c;
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}