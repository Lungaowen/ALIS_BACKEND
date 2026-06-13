package za.ac.alis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAsync
public class DemoApplication {

    public static void main(String[] args) {
        // Load .env file (ignore if not present – safe for Render)
        // The .env file lives in ./config — fall back to project root if that
        // directory doesn't exist (e.g. inside a Docker image).
        java.io.File configDir = new java.io.File("./config");
        Dotenv dotenv = Dotenv.configure()
                .directory(configDir.exists() ? "./config" : "./")
                .ignoreIfMissing()
                .load();

        // Push variables to system properties ONLY if they aren't already set
        // On Render, environment variables will take precedence.
        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(DemoApplication.class, args);
    }
}