package za.ac.alis.queries;

public final class LegalPractitionerQueries {

    private LegalPractitionerQueries() {}

    public static final String FIND_ALL = """
            SELECT lp.clientId   AS clientId,
                   lp.fullName   AS fullName,
                   lp.email      AS email,
                   lp.username   AS username,
                   lp.role       AS role,
                   lp.createdAt  AS createdAt,
                   lp.barNumber  AS barNumber,
                   lp.lawFirm    AS lawFirm
            FROM LegalPractitioner lp
            ORDER BY lp.createdAt DESC
            """;

    public static final String FIND_BY_ID = """
            SELECT lp.clientId   AS clientId,
                   lp.fullName   AS fullName,
                   lp.email      AS email,
                   lp.username   AS username,
                   lp.role       AS role,
                   lp.createdAt  AS createdAt,
                   lp.barNumber  AS barNumber,
                   lp.lawFirm    AS lawFirm
            FROM LegalPractitioner lp
            WHERE lp.clientId = :id
            """;
}
