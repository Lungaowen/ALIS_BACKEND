package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.entities.Document;
import za.ac.alis.entities.DocumentContent;
import java.util.Optional;

public interface DocumentContentRepository extends JpaRepository<DocumentContent, Long> {
    Optional<DocumentContent> findByDocument(Document document);  
}