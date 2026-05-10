package za.ac.alis.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.ac.alis.service.aiengine.AiEngineClient;
import za.ac.alis.dto.ChatRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    private static final Logger log = LoggerFactory.getLogger(CopilotController.class);
    private final AiEngineClient aiEngineClient;

    public CopilotController(AiEngineClient aiEngineClient) {
        this.aiEngineClient = aiEngineClient;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("Copilot chat request: question='{}', documentId={}, sessionId={}",
                request.getQuestion(), request.getDocumentId(), request.getSessionId());

        Map<String, Object> result = aiEngineClient.copilotChat(
                request.getQuestion(),
                request.getDocumentId(),
                request.getSessionId()
        );

        return ResponseEntity.ok(result);
    }
}