package za.ac.alis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ALIS Legal Compliance API")
                        .description("Automated Legal Intelligence System – South African compliance engine")
                        .version("1.0.0")
                        .license(new License()
                                .name("Proprietary")
                                .url("https://alis.co.za")));
    }
}