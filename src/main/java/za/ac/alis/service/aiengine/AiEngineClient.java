package za.ac.alis.service.aiengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class AiEngineClient {

    private static final Logger log = LoggerFactory.getLogger(AiEngineClient.class);
    private final RestTemplate restTemplate;
    private final String aiEngineUrl;

    public AiEngineClient(@Value("${alis.ai.engine.url}") String aiEngineUrl) {
        this.restTemplate = new RestTemplate();
        this.aiEngineUrl = aiEngineUrl;
    }

    public Map<String, Object> triggerAnalysis(Long documentId) {
        String url = aiEngineUrl + "/internal/analyze/" + documentId;
        try {
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            log.info("Analysis triggered for doc {}: {}", documentId, response);
            return response;
        } catch (Exception e) {
            log.error("Failed to trigger analysis for doc {}", documentId, e);
            throw new RuntimeException("AI engine analysis call failed", e);
        }
    }

    public Map<String, Object> getAnalysisStatus(Long documentId) {
        String url = aiEngineUrl + "/internal/status/" + documentId;
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> copilotChat(String question, Long documentId, String sessionId) {
        String url = aiEngineUrl + "/copilot/chat";
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("question", question);
        if (documentId != null) request.put("document_id", documentId);
        if (sessionId != null) request.put("session_id", sessionId);
        return restTemplate.postForObject(url, request, Map.class);
    }

    public Map<String, Object> similaritySearch(Long documentId, String queryText, int topK) {
        String url = aiEngineUrl + "/internal/similarity/search";
        Map<String, Object> body = new java.util.HashMap<>();
        if (documentId != null) body.put("document_id", documentId);
        if (queryText != null) body.put("query_text", queryText);
        body.put("top_k", topK);
        return restTemplate.postForObject(url, body, Map.class);
    }
}