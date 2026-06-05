package za.ac.alis.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

class JwtUtilTests {

    @Test
    void generatesAndParsesTokenWithConfiguredSecret() {
        JwtUtil jwtUtil = new JwtUtil("0123456789abcdef0123456789abcdef", 60_000);

        String token = jwtUtil.generateToken("42", "LEGAL_PRACTITIONER");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.parseClaims(token).getSubject()).isEqualTo("42");
        assertThat(jwtUtil.parseClaims(token).get("role", String.class)).isEqualTo("LEGAL_PRACTITIONER");
    }

    @Test
    void rejectsSecretsShorterThanThirtyTwoBytes() {
        // The JwtUtil constructor now accepts any length,
        // but Keys.hmacShaKeyFor() throws WeakKeyException for secrets < 256 bits (32 bytes).
        assertThrows(WeakKeyException.class,
                () -> new JwtUtil("too-short-secret", 60_000));
    }
}