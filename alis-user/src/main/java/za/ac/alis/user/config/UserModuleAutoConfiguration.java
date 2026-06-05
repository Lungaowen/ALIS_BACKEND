package za.ac.alis.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "alis.modules.user.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "za.ac.alis.user")
public class UserModuleAutoConfiguration {
}
