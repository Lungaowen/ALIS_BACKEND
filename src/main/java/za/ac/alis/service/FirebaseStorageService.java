package za.ac.alis.service;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.alis.utils.FileNameGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    private final Bucket  bucket;
    private final Storage storage;

    @Value("${firebase.bucket.name}")
    private String bucketName;

    public FirebaseStorageService(Bucket bucket) {
        this.bucket  = bucket;
        this.storage = bucket.getStorage();
    }

    // ── Result wrapper ─────────────────────────────────────────────────────────
    /**
     * Wraps both the stable object path (for downloads / AI pipeline)
     * and the short-lived signed URL (for immediate browser access).
     */
    public static class StorageResult {
        private final String objectPath; // e.g.  documents/client_3/1712345678_contract.pdf
        private final String signedUrl;  // 24-hour Firebase signed URL

        public StorageResult(String objectPath, String signedUrl) {
            this.objectPath = objectPath;
            this.signedUrl  = signedUrl;
        }

        public String getObjectPath() { return objectPath; }
        public String getSignedUrl()  { return signedUrl; }
    }

    // ── Upload (InputStream) ───────────────────────────────────────────────────
    public StorageResult uploadFile(InputStream inputStream,
                                    String fileName,
                                    String contentType,
                                    Long clientId) throws IOException {
        String   path     = buildPath(clientId, fileName);
        BlobId   blobId   = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setMetadata(Map.of(
                        "clientId",   clientId.toString(),
                        "uploadedAt", String.valueOf(System.currentTimeMillis())
                ))
                .build();

        log.info("Streaming upload '{}' → {} (client {})", fileName, path, clientId);
        storage.createFrom(blobInfo, inputStream);
        log.info("Upload complete: {}", path);
        return new StorageResult(path, generateSignedUrl(blobInfo));
    }

    // ── Upload (byte[]) ───────────────────────────────────────────────────────
    public StorageResult uploadFile(byte[] content,
                                    String fileName,
                                    String contentType,
                                    Long clientId) throws IOException {
        String   path     = buildPath(clientId, fileName);
        BlobId   blobId   = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setMetadata(Map.of(
                        "clientId",   clientId.toString(),
                        "uploadedAt", String.valueOf(System.currentTimeMillis())
                ))
                .build();

        log.info("Uploading '{}' → {} (client {})", fileName, path, clientId);
        storage.create(blobInfo, content);
        return new StorageResult(path, generateSignedUrl(blobInfo));
    }

    // ── Upload (MultipartFile convenience) ────────────────────────────────────
    public StorageResult uploadFile(MultipartFile file, Long clientId) throws IOException {
        String fileName = FileNameGenerator.generate(file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            return uploadFile(is, fileName, file.getContentType(), clientId);
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────
    public byte[] downloadFile(String objectPath) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectPath);
        Blob   blob   = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            throw new IOException("File not found in Firebase: " + objectPath);
        }
        return blob.getContent();
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public boolean deleteFileByPath(String objectPath) {
        boolean deleted = storage.delete(BlobId.of(bucketName, objectPath));
        if (deleted) log.info("Deleted: {}", objectPath);
        else         log.warn("Not found or already deleted: {}", objectPath);
        return deleted;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildPath(Long clientId, String fileName) {
        return "documents/client_" + clientId + "/"
                + System.currentTimeMillis() + "_" + fileName;
    }

    private String generateSignedUrl(BlobInfo blobInfo) {
        URL url = storage.signUrl(
                blobInfo, 24, TimeUnit.HOURS,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }
}
