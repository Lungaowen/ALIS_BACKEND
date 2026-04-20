package za.ac.alis.queries;

public final class AdminDashboardQueries {

    private AdminDashboardQueries() {}

    public static final String COUNT_ACTIVE_CLIENTS = """
            SELECT COUNT(DISTINCT d.client.clientId)
            FROM Document d
            """;

    public static final String FIND_CLIENT_ACTIVITIES = """
            SELECT c.clientId            AS clientId,
                   c.fullName            AS fullName,
                   c.email               AS email,
                   c.role                AS role,
                   c.createdAt           AS createdAt,
                   COUNT(d.documentId)   AS documentCount
            FROM Client c
            LEFT JOIN c.documents d
            GROUP BY c.clientId, c.fullName, c.email, c.role, c.createdAt
            ORDER BY COUNT(d.documentId) DESC
            """;

    public static final String FIND_RECENT_DOCUMENTS = """
            SELECT d.documentId      AS documentId,
                   d.title           AS title,
                   d.status          AS status,
                   d.ingestionSource AS ingestionSource,
                   d.uploadedAt      AS uploadedAt,
                   d.filePath        AS filePath,
                   d.fileUrl         AS fileUrl,
                   c.clientId        AS clientId,
                   c.fullName        AS clientName
            FROM Document d
            JOIN d.client c
            ORDER BY d.uploadedAt DESC
            """;

    public static final String FIND_RECENT_REPORTS = """
            SELECT r.reportId           AS reportId,
                   d.documentId         AS documentId,
                   d.title              AS documentTitle,
                   r.riskLevel          AS riskLevel,
                   r.analysisStatus     AS analysisStatus,
                   r.aiRecommendation   AS aiRecommendation,
                   r.generatedAt        AS generatedAt,
                   r.modelVersion       AS modelVersion
            FROM SummaryReport r
            JOIN r.document d
            ORDER BY r.generatedAt DESC
            """;

    public static final String ROLE_DISTRIBUTION = """
            SELECT c.role    AS role,
                   COUNT(c)  AS count
            FROM Client c
            GROUP BY c.role
            """;

    public static final String RISK_DISTRIBUTION = """
            SELECT r.riskLevel  AS riskLevel,
                   COUNT(r)     AS count
            FROM SummaryReport r
            GROUP BY r.riskLevel
            """;

   public static final String MONTHLY_UPLOADS = """
        SELECT EXTRACT(YEAR FROM d.uploadedAt) AS year,
               EXTRACT(MONTH FROM d.uploadedAt) AS month,
               COUNT(d) AS count
        FROM Document d
        WHERE d.uploadedAt >= :since
        GROUP BY EXTRACT(YEAR FROM d.uploadedAt), EXTRACT(MONTH FROM d.uploadedAt)
        ORDER BY EXTRACT(YEAR FROM d.uploadedAt), EXTRACT(MONTH FROM d.uploadedAt)
        """;
}
