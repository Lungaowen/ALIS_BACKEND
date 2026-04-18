package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.entities.LegalPractitioner;

public interface LegalPractitionerRepository extends JpaRepository<LegalPractitioner, Long> {
}