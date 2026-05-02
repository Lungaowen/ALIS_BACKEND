package za.ac.alis.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new JwtUtil("too-short-secret", 60_000));

        assertThat(thrown).hasMessageContaining("at least 32 bytes");
    }
}
