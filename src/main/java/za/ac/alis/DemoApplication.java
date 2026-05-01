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
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

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
