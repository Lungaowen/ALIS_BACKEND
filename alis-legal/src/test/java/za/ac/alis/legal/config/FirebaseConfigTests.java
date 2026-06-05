package za.ac.alis.legal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FirebaseConfigTests {

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyWhenServiceAccountValueIsBlank() throws Exception {
        Optional<InputStream> stream = FirebaseConfig.configuredServiceAccountStream("   ");

        assertThat(stream).isEmpty();
    }

    @Test
    void treatsJsonServiceAccountValueAsInlineCredentials() throws Exception {
        String credentials = "{\"type\":\"service_account\"}";

        try (InputStream stream = FirebaseConfig.configuredServiceAccountStream(credentials).orElseThrow()) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).isEqualTo(credentials);
        }
    }

    @Test
    void treatsNonJsonServiceAccountValueAsCredentialsFilePath() throws Exception {
        String credentials = "{\"type\":\"service_account\"}";
        Path credentialsFile = tempDir.resolve("firebase-service-account.json");
        Files.writeString(credentialsFile, credentials, StandardCharsets.UTF_8);

        try (InputStream stream = FirebaseConfig.configuredServiceAccountStream(credentialsFile.toString()).orElseThrow()) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).isEqualTo(credentials);
        }
    }

    @Test
    void rejectsNonJsonValueThatIsNotAReadableFilePath() {
        assertThatThrownBy(() -> FirebaseConfig.configuredServiceAccountStream("/missing/firebase-service-account.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not JSON and does not point to a readable credentials file");
    }
}