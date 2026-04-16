package za.ac.alis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.alis.entities.Client;
import za.ac.alis.repo.ClientRepository;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository repository;

    public Client createClient(Client client) {
        return repository.save(client);
    }

    public List<Client> getAllClients() {
        return repository.findAll();
    }

    public Client getClientByEmail(String email) {
        return repository.findByEmail(email);
    }
}