package za.ac.alis.service;

import za.ac.alis.config.SupabaseConfig;
import za.ac.alis.utils.FileNameGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
public class StorageService {

    private final SupabaseConfig supabaseConfig;
    private final HttpClient httpClient;

    public StorageService(SupabaseConfig supabaseConfig) {
        this.supabaseConfig = supabaseConfig;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Uploads a file to Supabase Storage and returns the public URL
     */
    public String uploadFile(MultipartFile file, Long clientId) throws IOException, InterruptedException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Generate unique filename
        String uniqueFileName = FileNameGenerator.generate(file.getOriginalFilename());

        // Build Supabase Storage URL
        String uploadUrl = supabaseConfig.getUrl() + "/storage/v1/object/" + 
                          supabaseConfig.getBucket() + "/" + uniqueFileName;

        // Prepare request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseConfig.getKey())
                .header("Content-Type", file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        // Upload file
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to upload file to Supabase. Status: " + response.statusCode());
        }

        // Return public URL
        return supabaseConfig.getStoragePublicUrl() + 
               supabaseConfig.getBucket() + "/" + uniqueFileName;
    }

    /**
     * Alternative: Upload with custom path (e.g., client-specific folder)
     */
    public String uploadFile(MultipartFile file, String folderPath) throws IOException, InterruptedException {
        String uniqueFileName = FileNameGenerator.generate(file.getOriginalFilename());
        String fullPath = folderPath.endsWith("/") ? folderPath + uniqueFileName : folderPath + "/" + uniqueFileName;

        String uploadUrl = supabaseConfig.getUrl() + "/storage/v1/object/" + 
                          supabaseConfig.getBucket() + "/" + fullPath;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseConfig.getKey())
                .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Upload failed. Status: " + response.statusCode());
        }

        return supabaseConfig.getStoragePublicUrl() + supabaseConfig.getBucket() + "/" + fullPath;
    }

    /**
     * Delete a file from Supabase Storage
     */
    public boolean deleteFile(String filePath) throws IOException, InterruptedException {
        String deleteUrl = supabaseConfig.getUrl() + "/storage/v1/object/" + 
                          supabaseConfig.getBucket() + "/" + filePath;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("Authorization", "Bearer " + supabaseConfig.getKey())
                .DELETE()
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() == 200;
    }
}