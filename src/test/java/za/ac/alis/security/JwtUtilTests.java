package za.ac.alis.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import za.ac.alis.config.EnvConfig;

class JwtUtilTests {

    // Use a valid Base64-encoded secret of at least 32 bytes (example: 32 random bytes)
    private static final String VALID_BASE64_SECRET = Base64.getEncoder().encodeToString(
            "my-32-byte-long-secret-for-testing".getBytes()
    );

    private JwtUtil jwtUtil = new JwtUtil(VALID_BASE64_SECRET, 86400000L);

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken("user123", "test@example.com", "LEGAL_PRACTITIONER");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));

        Claims claims = jwtUtil.parseClaims(token);
        assertEquals("user123", claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("LEGAL_PRACTITIONER", claims.get("user_role", String.class));
    }

    @Test
    void rejectsSecretsThatDecodeToLessThanThirtyTwoBytes() {
        // "short" decodes to 5 bytes (too short)
        String shortSecret = Base64.getEncoder().encodeToString("short".getBytes());
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil(shortSecret, 60_000));
        assertThat(thrown).hasMessageContaining("The specified key byte array is " +
                "not a valid size for HMAC-SHA256");
    }

    @Test
    void rejectsInvalidBase64String() {
        String invalidBase64 = "not-valid-base64!";
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil(invalidBase64, 60_000));
        assertThat(thrown).hasMessageContaining("Input byte array has incorrect ending byte");
    }
}