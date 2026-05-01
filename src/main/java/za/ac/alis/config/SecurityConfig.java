package za.ac.alis.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import za.ac.alis.security.JwtAuthenticationFilter;

/**
 * Security configuration for ALIS backend.
 *
 * CLIENT COMPATIBILITY NOTES:
 * ─────────────────────────────────────────────────────────────────────────────
 *  Browser (React, Vue, etc.)  → Subject to CORS. Configure allowed origins
 *                                via ALIS_CORS_ALLOWED_ORIGIN_PATTERNS env var.
 *
 *  Android                     → Not subject to CORS. Uses JWT directly.
 *                                No config changes needed here.
 *
 *  JavaFX desktop              → Not subject to CORS. Uses JWT directly.
 *                                No config changes needed here.
 *
 *  Any other native/CLI client → Not subject to CORS. Uses JWT directly.
 *                                No config changes needed here.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * All clients authenticate the same way:
 *   POST /api/auth/login  →  { "token": "..." }
 *   All subsequent requests: Authorization: Bearer <token>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${alis.cors.allowed-origin-patterns:*}") String allowedOriginPatterns) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/", "/health").permitAll()
                .requestMatchers("/api/auth/login").permitAll()

                // Admin only
                .requestMatchers("/api/auth/register").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")

                // Authenticated roles
                .requestMatchers("/api/client/**")
                    .hasAnyRole("USER", "LEGAL_PRACTITIONER", "DEAL_MAKER")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Applies only to browser clients — Android, JavaFX, and other
        // native clients are unaffected by this setting entirely.
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
