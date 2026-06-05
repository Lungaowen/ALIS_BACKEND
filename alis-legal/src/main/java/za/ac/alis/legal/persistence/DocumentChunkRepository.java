package za.ac.alis.legal.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.core.persistence.Document;
import za.ac.alis.core.persistence.DocumentChunk;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentDocumentIdOrderByChunkIndexAsc(Long documentId);

    @Transactional
    void deleteByDocument(Document document);
}
