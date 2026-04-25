package za.ac.alis.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import com.google.cloud.firestore.Firestore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_BUCKET_NAME}")
    private String storageBucket;

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${FIREBASE_SERVICE_ACCOUNT:}")
    private String credentialsJson;

    @PostConstruct
    public void init() throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {

            InputStream credentialsStream;

            if (credentialsJson != null && !credentialsJson.isEmpty()) {
                credentialsStream = new java.io.ByteArrayInputStream(
                        credentialsJson.getBytes(StandardCharsets.UTF_8)
                );
            } else {
                credentialsStream = new ClassPathResource(credentialsPath).getInputStream();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .setStorageBucket(storageBucket)
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }

    // 🪣 Storage (file system)
    @Bean
    public Bucket firebaseBucket() {
        return StorageClient.getInstance().bucket();
    }

    // 🧾 Firestore (database for ALIS)
    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }
}
