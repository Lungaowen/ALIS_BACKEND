package za.ac.alis.legal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.ac.alis.core.dto.RagAnswerDTO;
import za.ac.alis.core.dto.RagQueryRequest;
import za.ac.alis.legal.service.RagService;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RagAnswerDTO> ask(@RequestBody RagQueryRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        RagAnswerDTO answer = ragService.answer(
                request.getQuestion().trim(),
                request.getDocumentId(),
                request.getTopK());
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RagAnswerDTO> ask(
            @RequestParam("q") String question,
            @RequestParam(required = false) Long documentId,
            @RequestParam(required = false) Integer topK) {
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(ragService.answer(question.trim(), documentId, topK));
    }
}
