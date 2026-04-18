package za.ac.alis.repo;

import za.ac.alis.entities.SummaryReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryReportRepository extends JpaRepository<SummaryReport, Long> {
}