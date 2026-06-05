package za.ac.alis.legal.service;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import za.ac.alis.core.util.FileNameGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    private final Bucket bucket;
    private final Storage storage;
    private final String bucketName;

    public FirebaseStorageService(ObjectProvider<Bucket> firebaseBucketProvider) {
        this.bucket = firebaseBucketProvider.getIfAvailable();
        this.storage = bucket != null ? bucket.getStorage() : null;
        this.bucketName = bucket != null ? bucket.getName() : null;

        if (bucket != null) {
            log.info("FirebaseStorageService initialized with bucket: {}", bucketName);
        } else {
            log.warn("FirebaseStorageService initialized without a bucket — Firebase Storage is disabled.");
        }
    }

    // -------------------------------------------------------------------------
    // Guard helper — call at the top of every public method
    // -------------------------------------------------------------------------
    private void requireFirebase() {
        if (bucket == null) {
            throw new IllegalStateException(
                "Firebase Storage is not available in this environment. " +
                "Set alis.firebase.enabled=true and provide credentials to enable it."
            );
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static class StorageResult {
        private final String objectPath;
        private final String signedUrl;

        public StorageResult(String objectPath, String signedUrl) {
            this.objectPath = objectPath;
            this.signedUrl = signedUrl;
        }

        public String getObjectPath() { return objectPath; }
        public String getSignedUrl()  { return signedUrl; }
    }

    public StorageResult uploadFile(InputStream inputStream, String originalFileName,
                                    String contentType, Long clientId) throws IOException {
        requireFirebase();

        String safeFileName = FileNameGenerator.generate(originalFileName);
        String objectPath   = "documents/client_" + clientId + "/" + safeFileName;

        BlobId   blobId   = BlobId.of(bucketName, objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        storage.createFrom(blobInfo, inputStream);

        String signedUrl = generateSignedUrl(objectPath);
        log.info("Upload successful: {}", objectPath);
        return new StorageResult(objectPath, signedUrl);
    }

    public byte[] downloadFile(String objectPath) throws IOException {
        requireFirebase();

        Blob blob = storage.get(BlobId.of(bucketName, objectPath));
        if (blob == null || !blob.exists()) {
            throw new IOException("File not found in Firebase: " + objectPath);
        }
        return blob.getContent();
    }

    public String generateSignedUrl(String objectPath) {
        requireFirebase();

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath)).build();
        URL url = storage.signUrl(blobInfo, 24, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

    public boolean deleteFileByPath(String objectPath) {
        requireFirebase();

        return storage.delete(BlobId.of(bucketName, objectPath));
    }
}