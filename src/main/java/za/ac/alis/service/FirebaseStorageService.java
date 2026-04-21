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
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    private final Bucket bucket;
    private final Storage storage;

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    public FirebaseStorageService(Bucket bucket) {
        this.bucket = bucket;
        this.storage = bucket.getStorage();
    }

    /**
     * Upload using byte array (use when you already have the file content in memory).
     */
    public String uploadFile(byte[] content, String fileName, String contentType, Long clientId) throws IOException {
        String path = buildPath(clientId, fileName);
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        log.info("Uploading file '{}' for client {} to Firebase Storage", fileName, clientId);
        storage.create(blobInfo, content);
        log.info("Upload completed for '{}'", fileName);

        return generateSignedUrl(blobInfo);
    }

    /**
     * Upload using InputStream (preferred for large files to avoid memory overload).
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType, Long clientId) throws IOException {
        String path = buildPath(clientId, fileName);
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        log.info("Streaming upload for file '{}' (client {})", fileName, clientId);
        storage.createFrom(blobInfo, inputStream);
        log.info("Streaming upload completed for '{}'", fileName);

        return generateSignedUrl(blobInfo);
    }

    /**
     * Convenience method for MultipartFile – streams directly.
     */
    public String uploadFile(MultipartFile file, Long clientId) throws IOException {
        String fileName = FileNameGenerator.generate(file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            return uploadFile(is, fileName, file.getContentType(), clientId);
        }
    }

    /**
     * Delete a file from Firebase Storage.
     */
    public boolean deleteFile(String fileUrl) {
        String base = "https://storage.googleapis.com/" + bucketName + "/";
        if (!fileUrl.startsWith(base)) {
            log.warn("Attempted to delete file with non‑standard URL: {}", fileUrl);
            return false;
        }
        String objectPath = fileUrl.substring(base.length());
        BlobId blobId = BlobId.of(bucketName, objectPath);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("Deleted file from Firebase: {}", objectPath);
        } else {
            log.warn("Failed to delete file (may not exist): {}", objectPath);
        }
        return deleted;
    }

    private String buildPath(Long clientId, String fileName) {
        return "documents/client_" + clientId + "/" + fileName;
    }

    private String generateSignedUrl(BlobInfo blobInfo) {
        URL signedUrl = storage.signUrl(
                blobInfo,
                1, TimeUnit.HOURS,
                Storage.SignUrlOption.withV4Signature()
        );
        return signedUrl.toString();
    }
}