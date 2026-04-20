package za.ac.alis.queries;

public final class AuditLogQueries {

    private AuditLogQueries() {}

    // Aliases match AuditLogProjection getter names exactly
    public static final String FIND_ALL_LOGS = """
            SELECT a.logId          AS logId,
                   a.actionType     AS actionType,
                   a.description    AS description,
                   a.ipAddress      AS ipAddress,
                   a.createdAt      AS createdAt,
                   a.client.clientId AS clientId,
                   a.admin.adminId   AS adminId,
                   a.document.documentId AS documentId
            FROM AuditLog a
            ORDER BY a.createdAt DESC
            """;

    public static final String FIND_RECENT_LOGS = """
            SELECT a.logId          AS logId,
                   a.actionType     AS actionType,
                   a.description    AS description,
                   a.ipAddress      AS ipAddress,
                   a.createdAt      AS createdAt,
                   a.client.clientId AS clientId,
                   a.admin.adminId   AS adminId,
                   a.document.documentId AS documentId
            FROM AuditLog a
            ORDER BY a.createdAt DESC
            """;

    public static final String FIND_LOGS_BY_CLIENT_ID = """
            SELECT a.logId          AS logId,
                   a.actionType     AS actionType,
                   a.description    AS description,
                   a.ipAddress      AS ipAddress,
                   a.createdAt      AS createdAt,
                   a.client.clientId AS clientId,
                   a.admin.adminId   AS adminId,
                   a.document.documentId AS documentId
            FROM AuditLog a
            WHERE a.client.clientId = :clientId
            ORDER BY a.createdAt DESC
            """;
}
