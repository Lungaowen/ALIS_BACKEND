package za.ac.alis.legal.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.ai.service.RagGenerationService;
import za.ac.alis.core.dto.RagAnswerDTO;
import za.ac.alis.core.dto.RagSourceDTO;
import za.ac.alis.legal.persistence.search.DocumentChunkSearchProjection;
import za.ac.alis.legal.persistence.search.DocumentChunkSearchRepository;

@Service
public class RagService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;

    private final DocumentChunkSearchRepository chunkSearchRepository;
    private final ObjectProvider<RagGenerationService> ragGenerationService;

    public RagService(DocumentChunkSearchRepository chunkSearchRepository,
                      ObjectProvider<RagGenerationService> ragGenerationService) {
        this.chunkSearchRepository = chunkSearchRepository;
        this.ragGenerationService = ragGenerationService;
    }

    @Transactional(readOnly = true)
    public RagAnswerDTO answer(String question, Long documentId, Integer topK) {
        int limit = safeTopK(topK);
        Long clientId = resolveScopedClientId();

        List<RagSourceDTO> sources = chunkSearchRepository
                .searchChunks(question, clientId, documentId, limit)
                .stream()
                .map(this::toSource)
                .toList();

        if (sources.isEmpty()) {
            RagAnswerDTO answer = new RagAnswerDTO();
            answer.setQuestion(question);
            answer.setGrounded(false);
            answer.setSummary("No relevant document context was found.");
            answer.setAnswer("I could not find matching document chunks for that question.");
            answer.setSources(List.of());
            return answer;
        }

        RagGenerationService generator = ragGenerationService.getIfAvailable();
        if (generator == null) {
            RagAnswerDTO answer = new RagAnswerDTO();
            answer.setQuestion(question);
            answer.setGrounded(false);
            answer.setSummary("Relevant context was found, but AI generation is not configured.");
            answer.setAnswer("Set alis.ai.groq.key to enable grounded RAG answers.");
            answer.setSources(sources);
            return answer;
        }

        return generator.answer(question, sources);
    }

    private int safeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.min(Math.max(topK, 1), MAX_TOP_K);
    }

    private RagSourceDTO toSource(DocumentChunkSearchProjection projection) {
        RagSourceDTO source = new RagSourceDTO();
        source.setChunkId(projection.getChunkId());
        source.setDocumentId(projection.getDocumentId());
        source.setDocumentTitle(projection.getDocumentTitle());
        source.setChunkIndex(projection.getChunkIndex());
        source.setText(projection.getChunkText());
        source.setRank(projection.getRank());
        return source;
    }

    private Long resolveScopedClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        if (hasRole(authentication, "ADMIN") || hasRole(authentication, "LEGAL_PRACTITIONER")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String value) {
            return Long.valueOf(value);
        }
        throw new IllegalStateException("Authenticated principal is not a client id");
    }

    private boolean hasRole(Authentication authentication, String role) {
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
