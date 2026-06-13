package za.ac.alis.service;

import za.ac.alis.entities.Client;
import java.util.List;

public interface ClientServiceINT {

    Client createClient(Client client);

    List<Client> getAllClients();

    Client getClientByEmail(String email);

    Client getClientByUsername(String username);

    Client updateClient(Long id, Client updatedClient);

    void deleteClient(Long id);

    String generateUniqueUsername(String fullName);
}