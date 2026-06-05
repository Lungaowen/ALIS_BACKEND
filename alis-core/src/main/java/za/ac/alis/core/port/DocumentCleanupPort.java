package za.ac.alis.core.port;

public interface DocumentCleanupPort {

    void deleteAllDocumentsForClient(Long clientId);
}
