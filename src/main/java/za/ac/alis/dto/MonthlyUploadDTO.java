package za.ac.alis.dto;

import za.ac.alis.projections.MonthlyUploadProjection;

/**
 * Response DTO assembled from MonthlyUploadProjection.
 * label is pre-computed server-side — chart axis binds directly.
 */
public class MonthlyUploadDTO {

    private int    year;
    private int    month;
    private Long   count;
    private String label;

    private static final String[] MONTH_NAMES = {
        "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    // ── Built from projection in service ─────────────────────────────────────
    public static MonthlyUploadDTO from(MonthlyUploadProjection p) {
        MonthlyUploadDTO dto = new MonthlyUploadDTO();
        dto.year  = p.getYear();
        dto.month = p.getMonth();
        dto.count = p.getCount();
        dto.label = MONTH_NAMES[p.getMonth()] + " " + p.getYear();
        return dto;
    }

    public MonthlyUploadDTO() {}

    public int    getYear()  { return year; }
    public int    getMonth() { return month; }
    public Long   getCount() { return count; }
    public String getLabel() { return label; }
}
