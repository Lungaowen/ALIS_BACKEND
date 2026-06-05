package za.ac.alis.legal.port;

import org.springframework.stereotype.Component;

import za.ac.alis.core.port.DocumentCleanupPort;
import za.ac.alis.legal.service.DocumentService;

@Component
public class DocumentCleanupPortImpl implements DocumentCleanupPort {

    private final DocumentService documentService;

    public DocumentCleanupPortImpl(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void deleteAllDocumentsForClient(Long clientId) {
        documentService.getDocumentsByClientId(clientId).forEach(document ->
                documentService.deleteDocument(document.getDocumentId()));
    }
}
