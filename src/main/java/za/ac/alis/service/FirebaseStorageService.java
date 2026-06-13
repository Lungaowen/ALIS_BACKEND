package za.ac.alis.service;

import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.ac.alis.utils.FileNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FirebaseStorageService {

    private final Bucket bucket;
    private final Storage storage;
    private final String bucketName;
    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    public FirebaseStorageService(Bucket bucket) {
        this.bucket = bucket;
        this.storage = bucket.getStorage();
        this.bucketName = bucket.getName();
        log.info("FirebaseStorageService initialized with bucket: {}", bucketName);
    }

    public static class StorageResult {
        private final String objectPath;
        private final String signedUrl;

        public StorageResult(String objectPath, String signedUrl) {
            this.objectPath = objectPath;
            this.signedUrl = signedUrl;
        }

        public String getObjectPath() { return objectPath; }
        public String getSignedUrl() { return signedUrl; }
    }

    public StorageResult uploadFile(InputStream inputStream, String originalFileName, 
                                    String contentType, Long clientId) throws IOException {
        String safeFileName = FileNameGenerator.generate(originalFileName);
        String objectPath = "documents/client_" + clientId + "/" + safeFileName;

        BlobId blobId = BlobId.of(bucketName, objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.createFrom(blobInfo, inputStream);

        String signedUrl = generateSignedUrl(objectPath);
        log.info("Upload successful: {}", objectPath);
        return new StorageResult(objectPath, signedUrl);
    }

    public byte[] downloadFile(String objectPath) throws IOException {
        Blob blob = storage.get(BlobId.of(bucketName, objectPath));
        if (blob == null || !blob.exists()) {
            throw new IOException("File not found in Firebase: " + objectPath);
        }
        return blob.getContent();
    }

    public String generateSignedUrl(String objectPath) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath)).build();
        URL url = storage.signUrl(blobInfo, 24, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

    public boolean deleteFileByPath(String objectPath) {
        return storage.delete(BlobId.of(bucketName, objectPath));
    }
}