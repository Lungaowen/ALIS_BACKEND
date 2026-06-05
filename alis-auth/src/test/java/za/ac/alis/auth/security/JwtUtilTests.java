package za.ac.alis.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.WeakKeyException;

class JwtUtilTests {

    private static final String VALID_SECRET = "my-very-long-jwt-secret-key-which-is-32-bytes!";
    private static final String SHORT_SECRET = "short";

    @Test
    void generatesAndParsesTokenWithConfiguredSecret() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 86400000);
        String token = jwtUtil.generateToken("123", "LEGAL_PRACTITIONER");
        Claims claims = jwtUtil.parseClaims(token);
        
        assertThat(claims.getSubject()).isEqualTo("123");
        
        // Business role is stored in "app_role"
        assertThat(claims.get("app_role", String.class)).isEqualTo("LEGAL_PRACTITIONER");
        
        // The "role" claim is fixed to "authenticated" for PostgreSQL RLS
        assertThat(claims.get("role", String.class)).isEqualTo("authenticated");
    }

    @Test
    void rejectsSecretsShorterThanThirtyTwoBytes() {
        assertThrows(WeakKeyException.class, () -> new JwtUtil(SHORT_SECRET, 86400000));
    }
}