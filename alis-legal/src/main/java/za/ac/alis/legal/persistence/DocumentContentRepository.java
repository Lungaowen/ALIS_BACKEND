package za.ac.alis.legal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.DocumentContent;
import java.util.Optional;

public interface DocumentContentRepository extends JpaRepository<DocumentContent, Long> {
    Optional<DocumentContent> findByDocument(Document document);  
}