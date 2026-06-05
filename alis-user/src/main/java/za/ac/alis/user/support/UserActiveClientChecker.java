package za.ac.alis.user.support;

import org.springframework.stereotype.Component;

import za.ac.alis.core.port.ActiveClientChecker;
import za.ac.alis.user.persistence.ClientRepository;

@Component
public class UserActiveClientChecker implements ActiveClientChecker {

    private final ClientRepository clientRepository;

    public UserActiveClientChecker(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public boolean isActiveClient(String userId) {
        try {
            Long id = Long.parseLong(userId);
            return clientRepository.findById(id)
                    .map(client -> client.isActive())
                    .orElse(false);
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
