package za.ac.alis.core.projections;

/** Interface projection for monthly upload trend chart. */
public interface MonthlyUploadProjection {
    Integer getYear();
    Integer getMonth();
    Long    getCount();
}
