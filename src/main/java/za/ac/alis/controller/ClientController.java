package za.ac.alis.controller;

import org.springframework.web.bind.annotation.*;
import za.ac.alis.entities.Client;
import za.ac.alis.service.ClientService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Client")
@CrossOrigin(origins = "*")
public class ClientController {

    @Autowired
    private ClientService service;

    @PostMapping("/add")
    public Client addClient(@RequestBody Client client) {
        return service.createClient(client);
    }

    @GetMapping("/all")
    public List<Client> getAllClients() {
        return service.getAllClients();
    }

    @GetMapping("/{email}")
    public Client getByEmail(@PathVariable String email) {
        return service.getClientByEmail(email);
    }
}