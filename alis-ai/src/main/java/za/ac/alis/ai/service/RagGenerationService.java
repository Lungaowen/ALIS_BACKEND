package za.ac.alis.ai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import za.ac.alis.core.dto.RagAnswerDTO;
import za.ac.alis.core.dto.RagSourceDTO;

@Service
@ConditionalOnProperty(name = "alis.ai.groq.key")
public class RagGenerationService {

    private final String groqApiKey;
    private final String groqModel;
    private final String groqUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public RagGenerationService(
            @Value("${alis.ai.groq.key:}") String groqApiKey,
            @Value("${alis.ai.groq.model:llama-3.3-70b-versatile}") String groqModel,
            @Value("${alis.ai.groq.url:https://api.groq.com/openai/v1/chat/completions}") String groqUrl) {
        this.groqApiKey = groqApiKey;
        this.groqModel = groqModel;
        this.groqUrl = groqUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    public RagAnswerDTO answer(String question, List<RagSourceDTO> sources) {
        try {
            String prompt = buildPrompt(question, sources);
            String responseBody = callGroq(prompt);
            return parseAnswer(question, sources, responseBody);
        } catch (Exception e) {
            RagAnswerDTO fallback = new RagAnswerDTO();
            fallback.setQuestion(question);
            fallback.setGrounded(false);
            fallback.setSources(sources);
            fallback.setSummary("AI generation failed.");
            fallback.setAnswer("I found relevant document context, but the AI response failed: "
                    + e.getMessage());
            return fallback;
        }
    }

    private String buildPrompt(String question, List<RagSourceDTO> sources) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            RagSourceDTO source = sources.get(i);
            context.append("SOURCE ")
                    .append(i + 1)
                    .append(" | documentId=")
                    .append(source.getDocumentId())
                    .append(" | title=")
                    .append(source.getDocumentTitle())
                    .append(" | chunk=")
                    .append(source.getChunkIndex())
                    .append("\n")
                    .append(source.getText())
                    .append("\n\n");
        }

        return """
                You are ALIS, a South African legal compliance assistant.

                Use ONLY the document context below. If the context is insufficient,
                say that the available documents do not contain enough information.

                Return ONLY valid JSON with exactly these keys:
                {
                  "summary": "one concise paragraph",
                  "risks": ["risk 1", "risk 2"],
                  "recommendations": ["recommendation 1", "recommendation 2"],
                  "answer": "clear answer grounded in the supplied sources"
                }

                DOCUMENT CONTEXT:
                %s

                QUESTION:
                %s
                """.formatted(context.toString(), question);
    }

    private String callGroq(String prompt) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", groqModel);
        body.put("temperature", 0.1);
        body.put("max_tokens", 1200);
        body.putObject("response_format").put("type", "json_object");

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

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

    private RagAnswerDTO parseAnswer(
            String question,
            List<RagSourceDTO> sources,
            String rawResponse) throws Exception {
        JsonNode root = mapper.readTree(rawResponse);
        String content = root.path("choices").path(0)
                .path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Groq returned an empty completion");
        }

        content = content.replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)```\\s*$", "")
                .trim();

        JsonNode json = mapper.readTree(content);

        RagAnswerDTO answer = new RagAnswerDTO();
        answer.setQuestion(question);
        answer.setGrounded(true);
        answer.setSources(sources);
        answer.setSummary(json.path("summary").asText(""));
        answer.setAnswer(json.path("answer").asText(""));
        answer.setRisks(mapper.convertValue(
                json.path("risks"),
                mapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        answer.setRecommendations(mapper.convertValue(
                json.path("recommendations"),
                mapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        return answer;
    }
}
