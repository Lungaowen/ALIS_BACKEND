package za.ac.alis.core.projections;

/** Interface projection for monthly registration count. */
public interface MonthlyCountProjection {
    Integer getYear();
    Integer getMonth();
    Long    getCount();
}
