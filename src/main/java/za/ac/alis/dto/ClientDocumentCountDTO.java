package za.ac.alis.dto;

public class ClientDocumentCountDTO {

    private Long clientId;
    private String clientName;
    private Long documentCount;

    public ClientDocumentCountDTO(Long clientId, String clientName, Long documentCount) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.documentCount = documentCount;
    }

    public Long getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public Long getDocumentCount() { return documentCount; }
}