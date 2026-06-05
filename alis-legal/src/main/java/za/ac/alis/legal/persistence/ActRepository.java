// src/main/java/za/ac/alis/repo/ActRepository.java
package za.ac.alis.legal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.core.persistence.Act;
import java.util.Optional;

public interface ActRepository extends JpaRepository<Act, Long> {
    Optional<Act> findByActName(String actName);
}