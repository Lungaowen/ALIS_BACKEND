package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.alis.entities.DealMaker;
import za.ac.alis.projections.DealMakerProjection;

import java.util.List;
import java.util.Optional;

import static za.ac.alis.queries.DealMakerQueries.*;

@Repository
public interface DealMakerRepository extends JpaRepository<DealMaker, Long> {

    @Query(FIND_ALL)
    List<DealMakerProjection> findAllDealMakers();

    @Query(FIND_BY_ID)
    Optional<DealMakerProjection> findDealMakerById(@Param("id") Long id);
}
