package za.ac.alis.entities;

import jakarta.persistence.*;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.IngestionSource;
import java.time.LocalDateTime;

@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String title;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStat status = DocumentStat.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionSource ingestionSource = IngestionSource.MANUAL;

    private LocalDateTime archivedAt;

    // ====================== STORAGE REFERENCES ======================
   @Column(name = "file_path")   // Remove nullable = false
    private String filePath;        // Unique internal filename (e.g. doc_uuid_1712345678.pdf)

   
    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;           // Public URL from Supabase/Firebase

    @Column(name = "extracted_text_url")
    private String extractedTextUrl;

    @Column(name = "embedding_url")
    private String embeddingUrl;

    // One-to-One with FileMetadata (Document is the owner)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_metadata_id")
    private FileMetadata fileMetadata;

    public Document() {}

    // Getters and Setters
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public DocumentStat getStatus() { return status; }
    public void setStatus(DocumentStat status) { this.status = status; }

    public IngestionSource getIngestionSource() { return ingestionSource; }
    public void setIngestionSource(IngestionSource ingestionSource) { this.ingestionSource = ingestionSource; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getExtractedTextUrl() { return extractedTextUrl; }
    public void setExtractedTextUrl(String extractedTextUrl) { this.extractedTextUrl = extractedTextUrl; }

    public String getEmbeddingUrl() { return embeddingUrl; }
    public void setEmbeddingUrl(String embeddingUrl) { this.embeddingUrl = embeddingUrl; }

    public FileMetadata getFileMetadata() { return fileMetadata; }
    public void setFileMetadata(FileMetadata fileMetadata) { this.fileMetadata = fileMetadata; }
}