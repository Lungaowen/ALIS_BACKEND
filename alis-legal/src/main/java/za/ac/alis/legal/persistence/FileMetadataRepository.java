package za.ac.alis.legal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.FileMetadata;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    // Used for duplicate file detection in DocumentService
    boolean existsByHash(String hash);

    // Used by AiPipelineService to load file path + mimeType
    Optional<FileMetadata> findByDocument(Document document);
}