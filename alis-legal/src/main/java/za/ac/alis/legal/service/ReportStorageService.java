package za.ac.alis.legal.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.config.ReportStorageProperties;

@Service
public class ReportStorageService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final ReportStorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public ReportStorageService(ReportStorageProperties properties) {
        this.properties = properties;
        if (isConfigured()) {
            AwsCredentialsProvider credentialsProvider = credentialsProvider();
            Region region = Region.of(properties.getRegion().trim());

            S3ClientBuilder clientBuilder = S3Client.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider);
            S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider);

            if (hasText(properties.getEndpoint())) {
                URI endpoint = URI.create(properties.getEndpoint().trim());
                S3Configuration serviceConfiguration = S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build();
                clientBuilder.endpointOverride(endpoint).serviceConfiguration(serviceConfiguration);
                presignerBuilder.endpointOverride(endpoint).serviceConfiguration(serviceConfiguration);
            }

            this.s3Client = clientBuilder.build();
            this.presigner = presignerBuilder.build();
        } else {
            this.s3Client = null;
            this.presigner = null;
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && hasText(properties.getBucket())
                && hasText(properties.getRegion());
    }

    public StoredReport uploadReport(SummaryReport report, byte[] pdfBytes) {
        requireConfigured();

        String key = objectKey(report);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket().trim())
                .key(key)
                .contentType(PDF_CONTENT_TYPE)
                .contentDisposition("attachment; filename=\"Compliance_Report_" + report.getReportId() + ".pdf\"")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));
        SignedReport signedReport = sign(report, key);
        return new StoredReport(s3Uri(key), signedReport.signedUrl(), signedReport.expiresAt());
    }

    public SignedReport signReport(SummaryReport report) {
        requireConfigured();
        return sign(report, objectKey(report));
    }

    public String durableReportUrl(SummaryReport report) {
        requireConfigured();
        return s3Uri(objectKey(report));
    }

    private SignedReport sign(SummaryReport report, String key) {
        Instant expiresAt = Instant.now().plus(signatureDuration());
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket().trim())
                .key(key)
                .responseContentType(PDF_CONTENT_TYPE)
                .responseContentDisposition("attachment; filename=\"Compliance_Report_" + report.getReportId() + ".pdf\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration())
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        return new SignedReport(presigned.url().toString(), expiresAt);
    }

    private String objectKey(SummaryReport report) {
        if (report.getReportUrl() != null && report.getReportUrl().startsWith(s3UriPrefix())) {
            return report.getReportUrl().substring(s3UriPrefix().length());
        }
        if (report.getReportId() == null) {
            throw new IllegalArgumentException("Report must be saved before generating an S3 key");
        }
        Long clientId = report.getClient() != null ? report.getClient().getClientId() : null;
        Long documentId = report.getDocument() != null ? report.getDocument().getDocumentId() : null;
        String prefix = cleanPrefix(properties.getPrefix());
        return prefix
                + "/client_" + valueOrUnknown(clientId)
                + "/document_" + valueOrUnknown(documentId)
                + "/report_" + report.getReportId()
                + ".pdf";
    }

    private Duration signatureDuration() {
        long minutes = Math.max(1, properties.getSignedUrlDurationMinutes());
        return Duration.ofMinutes(minutes);
    }

    private String s3Uri(String key) {
        return s3UriPrefix() + key;
    }

    private String s3UriPrefix() {
        return "s3://" + properties.getBucket().trim() + "/";
    }

    private String cleanPrefix(String prefix) {
        String value = hasText(prefix) ? prefix.trim() : "reports";
        value = value.replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isBlank() ? "reports" : value;
    }

    private String valueOrUnknown(Long value) {
        return value != null ? value.toString() : "unknown";
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (hasText(properties.getAccessKeyId()) && hasText(properties.getSecretAccessKey())) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKeyId().trim(),
                    properties.getSecretAccessKey().trim());
            return StaticCredentialsProvider.create(credentials);
        }
        return DefaultCredentialsProvider.create();
    }

    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("S3 report storage is disabled");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("S3 report storage is enabled but bucket or region is missing");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @PreDestroy
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }

    public record StoredReport(String reportUrl, String signedUrl, Instant expiresAt) {
    }

    public record SignedReport(String signedUrl, Instant expiresAt) {
    }
}
