package za.ac.alis.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.alis.core.persistence.DealMaker;
import za.ac.alis.core.projections.DealMakerProjection;

import java.util.List;
import java.util.Optional;

import static za.ac.alis.core.queries.DealMakerQueries.*;

@Repository
public interface DealMakerRepository extends JpaRepository<DealMaker, Long> {

    @Query(FIND_ALL)
    List<DealMakerProjection> findAllDealMakers();

    @Query(FIND_BY_ID)
    Optional<DealMakerProjection> findDealMakerById(@Param("id") Long id);
}
