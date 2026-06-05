package za.ac.alis.core.queries;

public class DocumentQueries {

    // Count total distinct clients who have uploaded documents
    public static final String COUNT_DISTINCT_CLIENTS =
            "SELECT COUNT(DISTINCT d.client.clientId) FROM Document d";

    // Count documents per client (useful for activity report)
    public static final String COUNT_DOCUMENTS_BY_CLIENT =
            "SELECT COUNT(d) FROM Document d WHERE d.client.clientId = :clientId";

    // Find documents with client info (fetch join to avoid lazy issues)
    public static final String FIND_DOCUMENTS_BY_CLIENT =
            "SELECT d FROM Document d " +
            "LEFT JOIN FETCH d.client " +
            "WHERE d.client.clientId = :clientId " +
            "ORDER BY d.uploadedAt DESC";

    // Get all documents with basic info (optimized for reports)
    public static final String FIND_ALL_DOCUMENTS_WITH_CLIENT =
            "SELECT d FROM Document d " +
            "LEFT JOIN FETCH d.client " +
            "ORDER BY d.uploadedAt DESC";
}