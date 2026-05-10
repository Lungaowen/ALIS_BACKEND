package za.ac.alis.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import za.ac.alis.dto.ChatRequest;
import za.ac.alis.repo.DocumentRepository;
import za.ac.alis.service.GroqCopilotService;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    private static final Logger log = LoggerFactory.getLogger(CopilotController.class);
    private final GroqCopilotService groqCopilotService;
    private final DocumentRepository documentRepository;

    public CopilotController(GroqCopilotService groqCopilotService,
                             DocumentRepository documentRepository) {
        this.groqCopilotService = groqCopilotService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }

        log.info("Copilot chat request: question='{}', documentId={}, sessionId={}",
                request.getQuestion(), request.getDocumentId(), request.getSessionId());

        try {
            ensureDocumentAccess(request.getDocumentId());
            Map<String, Object> result = groqCopilotService.chat(
                    request.getQuestion(),
                    request.getDocumentId(),
                    request.getSessionId()
            );

            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    private void ensureDocumentAccess(Long documentId) {
        if (documentId == null) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (isAdmin) {
            return;
        }

        Long clientId;
        try {
            clientId = Long.valueOf(String.valueOf(auth.getPrincipal()));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to verify document access", e);
        }

        if (!documentRepository.existsByDocumentIdAndClientId(documentId, clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }
    }
}
