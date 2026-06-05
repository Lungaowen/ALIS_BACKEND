package za.ac.alis.core.dto;

public class ChatRequest {
    private String question;
    private Long documentId;
    private String sessionId;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}