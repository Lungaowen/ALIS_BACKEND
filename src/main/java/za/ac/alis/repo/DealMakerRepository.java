package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.entities.DealMaker;

public interface DealMakerRepository extends JpaRepository<DealMaker, Long> {
}