package za.ac.alis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.alis.entities.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Client findByEmail(String email);
}
