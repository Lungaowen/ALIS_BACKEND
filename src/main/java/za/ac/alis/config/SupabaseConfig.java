package za.ac.alis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseConfig {

    private String url;
    private String key;
    private String bucket;
    private String storagePublicUrl;   // e.g. https://.../storage/v1/object/public/

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getStoragePublicUrl() { return storagePublicUrl; }
    public void setStoragePublicUrl(String storagePublicUrl) { this.storagePublicUrl = storagePublicUrl; }
}