package za.ac.alis.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import za.ac.alis.entities.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    Optional<Client> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Optional<Client> getClientByClientId(Long client_Id);

    // Optional<Client> getClientByID(Long client_Id); 
    
}