package za.ac.alis.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "alis.modules.ai.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "za.ac.alis.ai")
public class AiModuleAutoConfiguration {
}
