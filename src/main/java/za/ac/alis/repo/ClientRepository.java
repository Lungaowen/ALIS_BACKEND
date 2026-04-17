package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.entities.Client;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    Optional<Client> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}