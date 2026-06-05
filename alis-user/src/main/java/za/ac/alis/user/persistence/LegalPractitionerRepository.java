package za.ac.alis.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.alis.core.persistence.LegalPractitioner;
import za.ac.alis.core.projections.LegalPractitionerProjection;

import java.util.List;
import java.util.Optional;

import static za.ac.alis.core.queries.LegalPractitionerQueries.*;

@Repository
public interface LegalPractitionerRepository extends JpaRepository<LegalPractitioner, Long> {

    @Query(FIND_ALL)
    List<LegalPractitionerProjection> findAllLegalPractitioners();

    @Query(FIND_BY_ID)
    Optional<LegalPractitionerProjection> findLegalPractitionerById(@Param("id") Long id);
}
