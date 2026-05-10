package za.ac.alis.dto;

/**
 * Request DTO for updating document metadata (PATCH /api/client/documents/{id}).
 * Only exposes fields a client is allowed to change — the file itself,
 * status, filePath, and fileUrl are all system-managed and excluded.
 */
public class DocumentUpdateRequest {

    private String title;

    public DocumentUpdateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
