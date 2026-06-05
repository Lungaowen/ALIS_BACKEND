package za.ac.alis.legal.persistence.search;

import java.time.LocalDateTime;

public interface DocumentSearchProjection {

    Long getDocumentId();

    String getTitle();

    String getStatus();

    LocalDateTime getUploadedAt();

    Long getClientId();

    Double getRank();
}
