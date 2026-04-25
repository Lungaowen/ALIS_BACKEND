package za.ac.alis.queries;

public final class AuditLogQueries {

    private AuditLogQueries() {}

    // LEFT JOIN preserves logs where client / admin / document is null
    public static final String FIND_ALL_LOGS = """
            SELECT a.logId          AS logId,
                   a.actionType     AS actionType,
                   a.description    AS description,
                   a.ipAddress      AS ipAddress,
                   a.createdAt      AS createdAt,
                   c.clientId       AS clientId,
                   ad.adminId       AS adminId,
                   doc.documentId   AS documentId
            FROM AuditLog a
            LEFT JOIN a.client   c
            LEFT JOIN a.admin    ad
            LEFT JOIN a.document doc
            ORDER BY a.createdAt DESC
            """;

    public static final String FIND_RECENT_LOGS = """
            SELECT a.logId          AS logId,
                   a.actionType     AS actionType,
                   a.description    AS description,
                   a.ipAddress      AS ipAddress,
                   a.createdAt      AS createdAt,
                   c.clientId       AS clientId,
                   ad.adminId       AS adminId,
                   doc.documentId   AS documentId
            FROM AuditLog a
            LEFT JOIN a.client   c
            LEFT JOIN a.admin    ad
            LEFT JOIN a.document doc
            ORDER BY a.createdAt DESC
            """;

    public static final String FIND_LOGS_BY_CLIENT_ID = """
            SELECT a.logId          AS logId,
                   a.actionType     AS actionType,
                   a.description    AS description,
                   a.ipAddress      AS ipAddress,
                   a.createdAt      AS createdAt,
                   c.clientId       AS clientId,
                   ad.adminId       AS adminId,
                   doc.documentId   AS documentId
            FROM AuditLog a
            LEFT JOIN a.client   c
            LEFT JOIN a.admin    ad
            LEFT JOIN a.document doc
            WHERE c.clientId = :clientId
            ORDER BY a.createdAt DESC
            """;
}
