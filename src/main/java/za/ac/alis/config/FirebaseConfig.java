package za.ac.alis.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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

    @Value("${firebase.bucket.name}")
    private String storageBucket;

    @Value("${firebase.service.account:}")
    private String serviceAccountJson;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        if (storageBucket == null || storageBucket.isBlank()) {
            throw new IllegalStateException("Firebase bucket name not configured. Set FIREBASE_BUCKET_NAME.");
        }

        InputStream serviceAccountStream = null;
        File secretFile = new File("/etc/secrets/firebase.json");

        if (secretFile.exists()) {
            log.info("Loading Firebase credentials from Render secret file");
            serviceAccountStream = new FileInputStream(secretFile);
        } else if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            log.info("Loading Firebase credentials from environment variable");
            serviceAccountStream = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }

        if (serviceAccountStream == null) {
            throw new IllegalStateException(
                    "Firebase credentials not found. Provide either a Render secret file or the FIREBASE_SERVICE_ACCOUNT env var.");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .setStorageBucket(storageBucket)
                .build();

        FirebaseApp.initializeApp(options);
        log.info("Firebase initialized successfully with bucket: {}", storageBucket);
    }

    @Bean
    public Bucket firebaseBucket() {
        return StorageClient.getInstance().bucket();
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }
}
