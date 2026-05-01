package za.ac.alis.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import za.ac.alis.entities.Clause;
import za.ac.alis.entities.Document;

@Repository
public interface ClauseRepository extends JpaRepository<Clause, Long> {

    List<Clause> findByDocument(Document document);

    List<Clause> findByDocument_DocumentId(Long documentId);

    /** Used by the pipeline to remove stale clauses before a fresh run. */
    @Transactional
    void deleteByDocument(Document document);
}
