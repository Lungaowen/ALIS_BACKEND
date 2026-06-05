package za.ac.alis.legal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "alis.modules.legal.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "za.ac.alis.legal")
public class LegalModuleAutoConfiguration {
}
