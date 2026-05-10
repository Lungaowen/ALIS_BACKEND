package za.ac.alis.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import za.ac.alis.entities.SummaryReport;
import za.ac.alis.repo.DocumentContentRepository;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.repo.SummaryReportRepository;

@Service
public class GroqCopilotService {

    private static final int MAX_CONTEXT_CHARS = 4_000;

    private final String groqApiKey;
    private final String groqModel;
    private final String groqUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final SummaryReportRepository summaryReportRepository;

    public GroqCopilotService(@Value("${alis.ai.groq.key:}") String groqApiKey,
                              @Value("${alis.ai.groq.model:llama-3.3-70b-versatile}") String groqModel,
                              @Value("${alis.ai.groq.url:https://api.groq.com/openai/v1/chat/completions}") String groqUrl,
                              DocumentRepository documentRepository,
                              DocumentContentRepository documentContentRepository,
                              SummaryReportRepository summaryReportRepository) {
        this.groqApiKey = groqApiKey;
        this.groqModel = groqModel;
        this.groqUrl = groqUrl;
        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.summaryReportRepository = summaryReportRepository;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    public Map<String, Object> chat(String question, Long documentId, String sessionId) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is not configured");
        }

        String prompt = buildPrompt(question, documentId);
        String answer = callGroq(prompt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer);
        response.put("response", answer);
        response.put("provider", "groq");
        response.put("model", groqModel);
        if (documentId != null) {
            response.put("documentId", documentId);
            response.put("document_id", documentId);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            response.put("sessionId", sessionId);
            response.put("session_id", sessionId);
        }
        return response;
    }

    private String buildPrompt(String question, Long documentId) {
        String context = "No document context was provided.";

        if (documentId != null) {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));

            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Document title: ").append(document.getTitle()).append("\n");
            contextBuilder.append("Document status: ").append(document.getStatus()).append("\n");

            documentContentRepository.findByDocument(document)
                    .map(DocumentContent::getExtractedText)
                    .filter(text -> !text.isBlank())
                    .ifPresent(text -> contextBuilder
                            .append("\nExtracted document text:\n")
                            .append(limit(text, MAX_CONTEXT_CHARS))
                            .append("\n"));

            summaryReportRepository.findFirstByDocument_DocumentId(documentId)
                    .ifPresent(report -> appendReportContext(contextBuilder, report));

            context = contextBuilder.toString();
        }

        return """
                You are ALIS Copilot, a concise South African legal compliance assistant.
                Use the supplied ALIS context when it is relevant. If the context is missing or insufficient, say what is missing.
                Do not invent legal facts or pretend to have reviewed a document that is not in the context.
                Keep the answer practical and clear.

                ========== ALIS CONTEXT ==========
                %s
                ========== END CONTEXT ==========

                User question:
                %s
                """.formatted(context, question);
    }

    private void appendReportContext(StringBuilder contextBuilder, SummaryReport report) {
        contextBuilder.append("\nLatest compliance report:\n");
        contextBuilder.append("Risk level: ").append(report.getRiskLevel()).append("\n");
        contextBuilder.append("Analysis status: ").append(report.getAnalysisStatus()).append("\n");
        contextBuilder.append("Similarity score: ").append(report.getSimilarityScore()).append("\n");
        if (report.getLawRule() != null) {
            contextBuilder.append("Primary rule: ").append(report.getLawRule().getKeyword()).append("\n");
            if (report.getLawRule().getAct() != null) {
                contextBuilder.append("Act: ").append(report.getLawRule().getAct().getActName()).append("\n");
            }
        }
        if (report.getAiExplanation() != null && !report.getAiExplanation().isBlank()) {
            contextBuilder.append("Explanation: ").append(limit(report.getAiExplanation(), 1_500)).append("\n");
        }
        if (report.getAiRecommendation() != null && !report.getAiRecommendation().isBlank()) {
            contextBuilder.append("Recommendation: ").append(limit(report.getAiRecommendation(), 1_500)).append("\n");
        }
    }

    private String callGroq(String prompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", groqModel);
            body.put("temperature", 0.2);
            body.put("max_tokens", 900);

            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqUrl))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Groq HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText().trim();
            if (content.isBlank()) {
                throw new RuntimeException("Groq returned an empty completion");
            }
            return content;
        } catch (Exception e) {
            throw new RuntimeException("Groq copilot call failed: " + e.getMessage(), e);
        }
    }

    private String limit(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n[...truncated...]";
    }
}
