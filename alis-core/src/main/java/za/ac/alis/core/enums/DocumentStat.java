package za.ac.alis.core.enums;

/**
 * Document processing lifecycle states.
 * PROCESSING is written by the Python pipeline during ML extraction
 * and must be tolerated by Hibernate when reading rows back.
 */
public enum DocumentStat {
    PENDING,
    PROCESSING,   // Python ML pipeline is running
    EXTRACTED,    // Pipeline complete, ready for compliance analysis
    ANALYZED,     // Groq compliance analysis complete
    FAILED,
    ARCHIVED
}
