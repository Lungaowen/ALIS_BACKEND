package za.ac.alis.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

import za.ac.alis.utils.FileNameGenerator;

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

    // Upload using byte array (used when you already have the content in memory)
    public String uploadFile(byte[] content, String fileName, String contentType, Long clientId) throws IOException {
        String path = buildPath(clientId, fileName);
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setMetadata(Map.of(
                        "clientId", clientId.toString(),
                        "uploadedAt", String.valueOf(System.currentTimeMillis())
                ))
                .build();
        log.info("Uploading file '{}' for client {} to Firebase Storage", fileName, clientId);
        storage.create(blobInfo, content);
        log.info("Upload completed for '{}'", fileName);
        return generateSignedUrl(blobInfo);
    }

    // Upload using InputStream (preferred for large files to avoid memory overload)
    public String uploadFile(InputStream inputStream, String fileName, String contentType, Long clientId) throws IOException {
        String path = buildPath(clientId, fileName);
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setMetadata(Map.of(
                        "clientId", clientId.toString(),
                        "uploadedAt", String.valueOf(System.currentTimeMillis())
                ))
                .build();
        log.info("Streaming upload for file '{}' (client {})", fileName, clientId);
        storage.createFrom(blobInfo, inputStream);
        log.info("Streaming upload completed for '{}'", fileName);
        return generateSignedUrl(blobInfo);
    }

    // Convenience method for MultipartFile – streams directly
    public String uploadFile(MultipartFile file, Long clientId) throws IOException {
        String fileName = FileNameGenerator.generate(file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            return uploadFile(is, fileName, file.getContentType(), clientId);
        }
    }

    // Download a file from Firebase Storage by its object path
    public byte[] downloadFile(String objectPath) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectPath);
        Blob blob = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            throw new IOException("File not found in Firebase: " + objectPath);
        }
        return blob.getContent();
    }

    // Delete a file using the stored object path (not the signed URL)
    public boolean deleteFileByPath(String objectPath) {
        BlobId blobId = BlobId.of(bucketName, objectPath);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("Deleted file from Firebase: {}", objectPath);
        } else {
            log.warn("File not found (or could not delete): {}", objectPath);
        }
        return deleted;
    }

    // Helper to build a unique path
    private String buildPath(Long clientId, String fileName) {
        // Ensure unique filenames – use a timestamp/UUID prefix
        String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
        return "documents/client_" + clientId + "/" + uniqueFileName;
    }

    // Generate a signed URL (temporary access)
    private String generateSignedUrl(BlobInfo blobInfo) {
        URL signedUrl = storage.signUrl(
                blobInfo,
                24, TimeUnit.HOURS,   // Extended to 24 hours for better UX
                Storage.SignUrlOption.withV4Signature()
        );
        return signedUrl.toString();
    }
}