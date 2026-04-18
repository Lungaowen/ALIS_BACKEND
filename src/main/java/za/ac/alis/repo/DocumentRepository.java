package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.entities.Document;
import za.ac.alis.enums.DocumentStat;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByClientClientId(Long clientId);

    List<Document> findByStatus(DocumentStat status);
}