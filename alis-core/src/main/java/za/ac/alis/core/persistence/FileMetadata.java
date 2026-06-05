package za.ac.alis.core.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long size;                 // in bytes

    @Column(nullable = false, unique = true)
    private String hash;               // SHA-256 recommended

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // No storagePath or fileUrl here — they belong in Document

    public FileMetadata() {}

    // Getters and Setters
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}