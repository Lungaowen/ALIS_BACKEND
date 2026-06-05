package za.ac.alis.core.projections;

import za.ac.alis.core.enums.DocumentStat;
import za.ac.alis.core.enums.IngestionSource;
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
