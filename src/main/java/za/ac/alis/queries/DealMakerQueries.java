package za.ac.alis.queries;

public final class DealMakerQueries {

    private DealMakerQueries() {}

    public static final String FIND_ALL = """
            SELECT d.clientId      AS clientId,
                   d.fullName      AS fullName,
                   d.email         AS email,
                   d.username      AS username,
                   d.role          AS role,
                   d.createdAt     AS createdAt,
                   d.companyName   AS companyName,
                   d.dealSpecialty AS dealSpecialty
            FROM DealMaker d
            ORDER BY d.createdAt DESC
            """;

    public static final String FIND_BY_ID = """
            SELECT d.clientId      AS clientId,
                   d.fullName      AS fullName,
                   d.email         AS email,
                   d.username      AS username,
                   d.role          AS role,
                   d.createdAt     AS createdAt,
                   d.companyName   AS companyName,
                   d.dealSpecialty AS dealSpecialty
            FROM DealMaker d
            WHERE d.clientId = :id
            """;
}
