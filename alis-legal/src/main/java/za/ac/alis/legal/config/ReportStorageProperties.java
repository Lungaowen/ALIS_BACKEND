package za.ac.alis.legal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportStorageProperties {

    @Value("${alis.reports.storage.s3.enabled:false}")
    private boolean enabled;

    @Value("${alis.reports.storage.s3.bucket:}")
    private String bucket;

    @Value("${alis.reports.storage.s3.region:us-east-1}")
    private String region;

    @Value("${alis.reports.storage.s3.prefix:reports}")
    private String prefix;

    @Value("${alis.reports.storage.s3.signed-url-duration-minutes:60}")
    private long signedUrlDurationMinutes;

    @Value("${alis.reports.storage.s3.endpoint:}")
    private String endpoint;

    @Value("${alis.reports.storage.s3.access-key-id:}")
    private String accessKeyId;

    @Value("${alis.reports.storage.s3.secret-access-key:}")
    private String secretAccessKey;

    public boolean isEnabled() {
        return enabled;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getSignedUrlDurationMinutes() {
        return signedUrlDurationMinutes;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }
}
