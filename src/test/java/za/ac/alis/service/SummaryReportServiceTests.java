package za.ac.alis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import za.ac.alis.entities.SummaryReport;
import za.ac.alis.repo.SummaryReportRepository;

class SummaryReportServiceTests {

    @Test
    void usesRepositoryMethodThatLoadsRelationsForDocumentLookups() {
        SummaryReportRepository repository = Mockito.mock(SummaryReportRepository.class);
        SummaryReportService service = new SummaryReportService(repository);
        List<SummaryReport> expected = List.of(new SummaryReport());
        when(repository.findByDocumentDocumentIdOrderByGeneratedAtDesc(7L)).thenReturn(expected);

        List<SummaryReport> result = service.findByDocumentIdWithRelations(7L);

        assertThat(result).isSameAs(expected);
        verify(repository).findByDocumentDocumentIdOrderByGeneratedAtDesc(7L);
    }
}
