package za.ac.alis.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_BUCKET_NAME}")
    private String storageBucket;

    @Value("${FIREBASE_SERVICE_ACCOUNT:}")
    private String serviceAccountJson;

    @PostConstruct
    public void init() throws IOException {

        if (!FirebaseApp.getApps().isEmpty()) return;

        InputStream serviceAccountStream;

        // ✅ PRIORITY 1: Render Secret File (BEST OPTION)
        File secretFile = new File("/etc/secrets/firebase.json");
        if (secretFile.exists()) {
            serviceAccountStream = new FileInputStream(secretFile);
        }

        // ✅ PRIORITY 2: ENV VAR (fallback)
        else if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            serviceAccountStream = new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8)
            );
        }

        // ❌ FAIL FAST (IMPORTANT)
        else {
            throw new IllegalStateException(
                    "Firebase credentials not found (Render secret file or env var missing)"
            );
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .setStorageBucket(storageBucket)
                .build();

        FirebaseApp.initializeApp(options);
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
