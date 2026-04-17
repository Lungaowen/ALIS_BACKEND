package za.ac.alis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.alis.entities.Client;
import za.ac.alis.repo.ClientRepository;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class ClientService implements ClientServiceINT {

    @Autowired
    private ClientRepository repository;

    @Override
    public Client createClient(Client client) {

         // 🔥 generate username first
        String username = generateUniqueUsername(client.getFullName());

        client.setUsername(username);

        return repository.save(client);
    }
    @Override
    public List<Client> getAllClients() {
        return repository.findAll();
    }
    @Override
    public Client getClientByEmail(String email) {
    return repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    @Override   
    public Client updateClient(Long id, Client updatedClient) {
    Client existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Client not found"));

    existing.setFullName(updatedClient.getFullName());
    existing.setEmail(updatedClient.getEmail());
    existing.setPasswordHash(updatedClient.getPasswordHash());
    existing.setRole(updatedClient.getRole());
    // add other fields

    return repository.save(existing);
    }
    @Override
    public void deleteClient(Long id) {
    if (!repository.existsById(id)) {
        throw new RuntimeException("Client not found");
    }
    repository.deleteById(id);
    }
    @Override
    public Client getClientByUsername(String username) {
    return repository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Client not found"));
    }
    @Override
    public String generateUniqueUsername(String fullName) {

    String base = fullName.toLowerCase().replaceAll(" ", "");
    String username;
    
    do {
        int randomNum = (int)(Math.random() * 10000);
        username = base + "_" + randomNum;
    } while (repository.existsByUsername(username));

    return username;
}
}