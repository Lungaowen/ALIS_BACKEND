package za.ac.alis.projections;

import za.ac.alis.enums.DocumentStat;
import za.ac.alis.enums.IngestionSource;
import java.time.LocalDateTime;

/**
 * Interface projection for document rows.
 * No broken setters, no UnsupportedOperationException.
 */
public interface DocumentInfoProjection {
    Long            getDocumentId();
    String          getTitle();
    DocumentStat    getStatus();
    IngestionSource getIngestionSource();
    LocalDateTime   getUploadedAt();
    String          getFilePath();
    String          getFileUrl();
    Long            getClientId();
    String          getClientName();
}
