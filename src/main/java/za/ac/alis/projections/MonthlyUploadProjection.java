package za.ac.alis.projections;

/** Interface projection for monthly upload trend chart. */
public interface MonthlyUploadProjection {
    Integer getYear();
    Integer getMonth();
    Long    getCount();
}
