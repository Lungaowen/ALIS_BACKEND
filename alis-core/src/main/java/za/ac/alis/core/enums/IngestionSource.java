package za.ac.alis.core.enums;

/**
 * How the document entered the system.
 * PYTHON_GATEWAY covers uploads sent directly to the Python FastAPI service.
 */
public enum IngestionSource {
    MANUAL,
    WATCHED_FOLDER,
    API,
    UPLOAD,
    PYTHON_GATEWAY  // uploaded via Python /api/process endpoint
}
