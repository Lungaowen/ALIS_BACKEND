package za.ac.alis.legal.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;

import jakarta.annotation.PostConstruct;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${alis.firebase.enabled}")
    private boolean firebaseEnabled;

    @Value("${firebase.bucket.name:}")
    private String storageBucket;

    @Value("${firebase.service.account:}")
    private String serviceAccountJson;

    @PostConstruct
    public void init() throws IOException {
        if (!firebaseEnabled) {
            log.info("Firebase is disabled for this environment");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        if (storageBucket == null || storageBucket.isBlank()) {
            throw new IllegalStateException("Firebase bucket name not configured. Set firebase.bucket.name.");
        }

        FirebaseOptions options;
        try (InputStream serviceAccountStream = openServiceAccountStream()) {
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .setStorageBucket(storageBucket)
                    .build();
        }

        FirebaseApp.initializeApp(options);
        log.info("Firebase initialized successfully with bucket: {}", storageBucket);
    }

    private InputStream openServiceAccountStream() throws IOException {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            String trimmed = serviceAccountJson.trim();
            if (looksLikeJson(trimmed)) {
                log.info("Loading Firebase credentials from firebase.service.account JSON");
            } else {
                log.info("Loading Firebase credentials from firebase.service.account file path");
            }
            return configuredServiceAccountStream(serviceAccountJson)
                    .orElseThrow(() -> new IllegalStateException(
                            "Invalid firebase.service.account – not JSON and not a readable file."));
        }

        ClassPathResource classPathResource = new ClassPathResource("firebase-service-account.json");
        if (classPathResource.exists()) {
            log.info("Loading Firebase credentials from classpath: firebase-service-account.json");
            return classPathResource.getInputStream();
        }

        throw new IllegalStateException(
                "Firebase credentials not found. Set firebase.service.account (JSON or file path) "
                        + "or place firebase-service-account.json in src/main/resources.");
    }

    // Package-private static method for testing and internal use
    static Optional<InputStream> configuredServiceAccountStream(String configuredValue) throws IOException {
        if (configuredValue == null || configuredValue.isBlank()) {
            return Optional.empty();
        }

        String trimmed = configuredValue.trim();
        if (looksLikeJson(trimmed)) {
            return Optional.of(new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8)));
        }

        File credentialsFile = new File(trimmed);
        if (!credentialsFile.isFile()) {
            throw new IllegalStateException(
                    "firebase.service.account is not JSON and does not point to a readable credentials file.");
        }

        return Optional.of(new FileInputStream(credentialsFile));
    }

    private static boolean looksLikeJson(String value) {
        return value.startsWith("{");
    }

    @Bean
    public Bucket firebaseBucket() {
        if (!firebaseEnabled) {
            log.warn("Firebase is disabled; no Bucket bean will be created.");
            return null;
        }
        return StorageClient.getInstance().bucket();
    }

    @Bean
    public Firestore firestore() {
        if (!firebaseEnabled) {
            log.warn("Firebase is disabled; no Firestore bean will be created.");
            return null;
        }
        return FirestoreClient.getFirestore();
    }
}