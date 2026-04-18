package za.ac.alis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ Disable CSRF — not needed for stateless REST API testing
            .csrf(csrf -> csrf.disable())

            // ✅ Enable CORS with our config below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ✅ Allow ALL requests — no auth required (testing mode)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // ✅ Disable Spring's default login page
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());  // disables the popup asking for password

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Allow these origins (add your frontend URL here later)
        config.setAllowedOriginPatterns(List.of("*"));

        // ✅ Allow all HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // ✅ Allow all headers (including Authorization for later JWT)
        config.setAllowedHeaders(List.of("*"));

        // ✅ Allow credentials (needed for JWT cookies later)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}