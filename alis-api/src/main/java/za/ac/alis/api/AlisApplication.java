package za.ac.alis.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(
        scanBasePackages = "za.ac.alis",
        exclude = UserDetailsServiceAutoConfiguration.class
)
@EntityScan(basePackages = "za.ac.alis.core.persistence")
@EnableJpaRepositories(basePackages = {
        "za.ac.alis.user.persistence",
        "za.ac.alis.legal.persistence"
})
@EnableAsync
public class AlisApplication {

    public static void main(String[] args) {
        LocalDotenvLoader.load();
        SpringApplication.run(AlisApplication.class, args);
    }
}
