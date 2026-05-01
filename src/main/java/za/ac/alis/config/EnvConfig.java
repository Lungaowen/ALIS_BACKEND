package za.ac.alis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@Configuration
@Profile("!prod")   // Only load .env in local/dev, not on Render (prod)
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")           // Look for .env in project root
                    .ignoreIfMissing()         // ← VERY IMPORTANT for Render
                    .load();

            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });

            System.out.println("✅ .env file loaded successfully for local development");
        } catch (Exception e) {
            System.out.println("⚠️ Could not load .env file (normal on Render): " + e.getMessage());
        }
    }
}