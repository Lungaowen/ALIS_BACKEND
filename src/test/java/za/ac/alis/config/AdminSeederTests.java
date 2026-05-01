package za.ac.alis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import za.ac.alis.entities.Admin;
import za.ac.alis.repo.AdminRepository;

class AdminSeederTests {

    @Test
    void skipsSeedingWhenBootstrapCredentialsAreMissing() throws Exception {
        AdminRepository adminRepository = org.mockito.Mockito.mock(AdminRepository.class);
        AdminSeeder seeder = new AdminSeeder(adminRepository, "System Admin", "", "");

        seeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void seedsAdminWhenBootstrapCredentialsAreConfigured() throws Exception {
        AdminRepository adminRepository = org.mockito.Mockito.mock(AdminRepository.class);
        when(adminRepository.findByEmail("bootstrap@example.com")).thenReturn(Optional.empty());
        AdminSeeder seeder = new AdminSeeder(adminRepository, "Bootstrap Admin", "bootstrap@example.com",
                "super-secret-password");

        seeder.run();

        ArgumentCaptor<Admin> savedAdmin = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(savedAdmin.capture());
        assertThat(savedAdmin.getValue().getEmail()).isEqualTo("bootstrap@example.com");
        assertThat(savedAdmin.getValue().getFullName()).isEqualTo("Bootstrap Admin");
        assertThat(savedAdmin.getValue().getPasswordHash()).isNotEqualTo("super-secret-password");
    }
}
