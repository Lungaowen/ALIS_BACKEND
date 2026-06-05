package za.ac.alis.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import za.ac.alis.api.LocalDotenvLoader;

@Configuration
@Profile("!prod")
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        if (LocalDotenvLoader.load()) {
            System.out.println(".env file loaded for local development");
        }
    }
}
