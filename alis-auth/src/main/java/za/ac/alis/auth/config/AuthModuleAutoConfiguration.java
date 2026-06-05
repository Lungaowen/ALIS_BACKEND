package za.ac.alis.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "alis.modules.auth.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "za.ac.alis.auth")
@Import(AuthSecurityBeans.class)
public class AuthModuleAutoConfiguration {
}
