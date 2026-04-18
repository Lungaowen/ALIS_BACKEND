package za.ac.alis.entities;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.IngestionSource;

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
    private DocumentStat status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionSource ingestionSource;

    private LocalDateTime archivedAt;

    // ── Related entities (1-to-1) ─────────────────────────────────────────────
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL)
    private FileMetadata fileMetadata;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL)
    private DocumentContent documentContent;

    // ── Related entities (1-to-many) ──────────────────────────────────────────
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<SummaryReport> summaryReports;

    // ❌ REMOVED: extractedText — it belongs in DocumentContent, NOT here
    //    Having it here breaks 3NF and caused the duplicate setExtractedText() error

    public Document() {}

    // =========================================================================
    // GETTERS AND SETTERS
    // =========================================================================

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public DocumentStat getStatus() {
        return status;
    }

    public void setStatus(DocumentStat status) {
        this.status = status;
    }

    public IngestionSource getIngestionSource() {
        return ingestionSource;
    }

    public void setIngestionSource(IngestionSource ingestionSource) {
        this.ingestionSource = ingestionSource;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public DocumentContent getDocumentContent() {
        return documentContent;
    }

    public void setDocumentContent(DocumentContent documentContent) {
        this.documentContent = documentContent;
    }

    public List<SummaryReport> getSummaryReports() {
        return summaryReports;
    }

    public void setSummaryReports(List<SummaryReport> summaryReports) {
        this.summaryReports = summaryReports;
    }
}