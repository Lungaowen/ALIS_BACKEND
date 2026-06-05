package za.ac.alis.ai.service;

import org.springframework.stereotype.Service;
import za.ac.alis.core.persistence.Act;
import za.ac.alis.core.persistence.LawRul;
import za.ac.alis.core.enums.RiskLevel;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
public class RuleExtractionService {

    private static final Pattern SUBSECTION_HEADER = Pattern.compile(
        "\\d+\\.\\s+[A-Z][^\\.]*"
    );

    private static final Pattern OBLIGATION = Pattern.compile(
        "\\b(must(\\s+not)?|shall(\\s+not)?|may\\s+not|require[sd]?\\b|prohibit(ed|s)?|oblige[sd]?|" +
        "mandatory|duty\\s+to|obligation\\s+to|not\\s+permitted|unlawful|offence|penalty|liable\\s+to|" +
        "entitled\\s+to|has\\s+(a\\s+)?(right|duty)\\s+to)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> ALLOWED_CHAPTERS = Set.of("CHAPTER 2", "CHAPTER 3", "CHAPTER 4");

    public List<LawRul> extractRules(String actText, Act act) {
        List<LawRul> rules = new ArrayList<>();
        List<String> chapters = splitByChapter(actText);

        for (String chapterBlock : chapters) {
            String chapterLabel = extractChapterLabel(chapterBlock);
            if (!ALLOWED_CHAPTERS.contains(chapterLabel)) continue;

            List<SectionSegment> segments = splitIntoSections(chapterBlock);
            for (SectionSegment segment : segments) {
                List<String> sentences = splitSentences(segment.content);
                for (String sentence : sentences) {
                    if (sentence.isBlank()) continue;
                    String trimmed = sentence.trim();

                    if (trimmed.split("\\s+").length < 5) continue;
                    String lower = trimmed.toLowerCase();
                    if (lower.matches("^'.*'\\s+means\\s+.*") ||
                        lower.startsWith("this section does not apply")) continue;

                    Matcher m = OBLIGATION.matcher(lower);
                    if (!m.find()) continue;

                    if (lower.startsWith("part ") || lower.startsWith("chapter ") ||
                        lower.startsWith("consumer’s right to") || lower.startsWith("right to")) continue;

                    String keyword = m.group(1).toLowerCase();
                    RiskLevel risk = classifyRisk(trimmed, keyword);
                    String suggestion = buildSuggestion(segment.sectionRef, trimmed, keyword);

                    LawRul rule = new LawRul();
                    rule.setAct(act);
                    rule.setKeyword(keyword);
                    rule.setRequirements(trimmed);
                    rule.setRiskLevel(risk);
                    rule.setSuggestion(suggestion);
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    private List<String> splitByChapter(String text) {
        List<String> blocks = new ArrayList<>();
        Matcher m = Pattern.compile("(CHAPTER \\d+).*?(?=CHAPTER \\d+|$)", Pattern.DOTALL).matcher(text);
        while (m.find()) {
            blocks.add(m.group());
        }
        return blocks;
    }

    private String extractChapterLabel(String block) {
        Matcher m = Pattern.compile("(CHAPTER \\d+)").matcher(block);
        return m.find() ? m.group(1) : "";
    }

    private List<SectionSegment> splitIntoSections(String text) {
        List<SectionSegment> segments = new ArrayList<>();
        Matcher headerMatcher = SUBSECTION_HEADER.matcher(text);
        int lastEnd = 0;
        String currentSection = "Preamble";

        while (headerMatcher.find()) {
            if (lastEnd < headerMatcher.start()) {
                String content = text.substring(lastEnd, headerMatcher.start()).trim();
                if (!content.isEmpty()) {
                    segments.add(new SectionSegment(currentSection, content));
                }
            }
            currentSection = headerMatcher.group().trim();
            lastEnd = headerMatcher.end();
        }
        if (lastEnd < text.length()) {
            segments.add(new SectionSegment(currentSection, text.substring(lastEnd).trim()));
        }
        return segments;
    }

    private List<String> splitSentences(String text) {
        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private RiskLevel classifyRisk(String sentence, String keyword) {
        String lower = sentence.toLowerCase();
        if (lower.matches(".*\\b(prohibit|offence|penalty|void|must not|shall not|may not|unlawful|fine|imprisonment|liable to|prohibited conduct)\\b.*")) {
            return RiskLevel.HIGH;
        }
        if (lower.matches(".*\\b(must|shall|require|oblige|mandatory|duty to|must ensure|must provide|must take|must comply)\\b.*")) {
            if (lower.matches(".*\\b(right to|entitled to|has the right|has a right)\\b.*")) {
                return RiskLevel.LOW;
            }
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String buildSuggestion(String sectionRef, String sentence, String keyword) {
        String action = switch (keyword.toLowerCase()) {
            case "must", "shall", "must ensure", "must provide", "must take", "must comply" -> "Implement procedure to: ";
            case "prohibit", "must not", "shall not", "may not", "unlawful" -> "Prohibit: ";
            case "liable to", "offence", "penalty" -> "Ensure no occurrence of: ";
            case "entitled to", "right to", "has a right to", "has the right to" -> "Guarantee consumer right: ";
            default -> "Address obligation: ";
        };
        return action + sentence + " (See " + sectionRef + ")";
    }

    private static class SectionSegment {
        String sectionRef;
        String content;
        SectionSegment(String ref, String content) {
            this.sectionRef = ref;
            this.content = content;
        }
    }
}