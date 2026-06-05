package za.ac.alis.user.api;

import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.persistence.DealMaker;
import za.ac.alis.core.persistence.LegalPractitioner;

public interface UserRegistrationService {

    Client registerClient(String fullName, String email, String password);

    DealMaker registerDealMaker(String fullName, String email, String password,
                                String companyName, String dealSpecialty);

    LegalPractitioner registerLegalPractitioner(String fullName, String email, String password,
                                                String barNumber, String lawFirm);
}
