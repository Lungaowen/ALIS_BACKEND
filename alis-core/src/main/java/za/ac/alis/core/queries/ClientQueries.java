package za.ac.alis.core.queries;

public final class ClientQueries {

    private ClientQueries() {}

    // ─────────────────────────────────────────────
    // Projection queries use column aliases that match
    // the projection interface getter names exactly.
    // Spring Data does the mapping — no constructor needed.
    // ─────────────────────────────────────────────

    public static final String FIND_ALL_CLIENTS = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            ORDER BY c.createdAt DESC
            """;

    public static final String SEARCH_CLIENTS = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.username) LIKE LOWER(CONCAT('%', :query, '%'))
            """;

    public static final String FILTER_CLIENTS = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            WHERE (:role IS NULL OR c.role = :role)
              AND (:from IS NULL OR c.createdAt >= :from)
              AND (:to   IS NULL OR c.createdAt <= :to)
            """;

    public static final String FIND_BY_ROLE = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            WHERE c.role = :role
            """;

    public static final String FIND_BY_DATE_RANGE = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            WHERE c.createdAt BETWEEN :from AND :to
            ORDER BY c.createdAt DESC
            """;

    public static final String FIND_RECENTLY_REGISTERED = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            WHERE c.createdAt >= :since
            ORDER BY c.createdAt DESC
            """;

    public static final String FIND_CLIENTS_WITH_NO_DOCUMENTS = """
            SELECT c.clientId   AS clientId,
                   c.fullName   AS fullName,
                   c.email      AS email,
                   c.username   AS username,
                   c.role       AS role,
                   c.createdAt  AS createdAt
            FROM Client c
            WHERE c.documents IS EMPTY
            """;

    // ─────────────────────────────────────────────
    // DOCUMENT COUNTS
    // ─────────────────────────────────────────────

    public static final String COUNT_DOCUMENTS_BY_CLIENT_ID = """
            SELECT COUNT(d.documentId)
            FROM Document d
            WHERE d.client.clientId = :clientId
            """;

    public static final String COUNT_DOCUMENTS_PER_CLIENT = """
            SELECT c.clientId    AS clientId,
                   c.fullName    AS fullName,
                   COUNT(d.documentId) AS documentCount
            FROM Client c
            LEFT JOIN c.documents d
            GROUP BY c.clientId, c.fullName
            """;

    // ─────────────────────────────────────────────
    // REPORTS
    // ─────────────────────────────────────────────

    public static final String COUNT_BY_ROLE = """
            SELECT c.role        AS role,
                   COUNT(c)      AS count
            FROM Client c
            GROUP BY c.role
            """;

    public static final String COUNT_REGISTRATIONS_BY_MONTH = """
        SELECT EXTRACT(YEAR FROM c.createdAt) AS year,
               EXTRACT(MONTH FROM c.createdAt) AS month,
               COUNT(c) AS count
        FROM Client c
        WHERE c.createdAt >= :since
        GROUP BY EXTRACT(YEAR FROM c.createdAt), EXTRACT(MONTH FROM c.createdAt)
        ORDER BY EXTRACT(YEAR FROM c.createdAt), EXTRACT(MONTH FROM c.createdAt)
        """;

    public static final String FIND_TOP_UPLOADERS = """
            SELECT c.clientId             AS clientId,
                   c.fullName             AS fullName,
                   c.email                AS email,
                   c.role                 AS role,
                   COUNT(d.documentId)    AS documentCount
            FROM Client c
            LEFT JOIN c.documents d
            GROUP BY c.clientId, c.fullName, c.email, c.role
            ORDER BY COUNT(d.documentId) DESC
            """;
}
