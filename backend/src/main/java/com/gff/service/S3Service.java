package com.gff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Service for uploading documents directly to AWS S3
 * and supporting presigned URL uploads.
 */
@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.bucket-name:visiting-card-bkt}")
    private String bucketName;

    @Value("${aws.s3.region:ap-south-1}")
    private String region;

    private final HttpClient httpClient;

    public S3Service() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }

    public boolean hasValidCredentials() {
        return accessKey != null && !accessKey.trim().isEmpty()
                && secretKey != null && !secretKey.trim().isEmpty();
    }

    public S3Client buildS3Client() {
        try {
            if (hasValidCredentials()) {
                return S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey.trim(), secretKey.trim())
                        ))
                        .build();
            } else {
                try {
                    DefaultCredentialsProvider provider = DefaultCredentialsProvider.create();
                    provider.resolveCredentials();
                    return S3Client.builder()
                            .region(Region.of(region))
                            .credentialsProvider(provider)
                            .build();
                } catch (Exception ex) {
                    log.warn("DefaultCredentialsProvider could not resolve credentials: {}", ex.getMessage());
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build S3Client: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Uploads in-memory byte array directly to AWS S3 bucket.
     *
     * @param data        Raw file bytes
     * @param objectKey   S3 key (e.g. "visiting-cards/REC-123.jpg")
     * @param contentType MIME type (e.g. "image/jpeg")
     * @return Public/Object URL of the uploaded S3 asset
     */
    public String uploadDirectToS3(byte[] data, String objectKey, String contentType) {
        if (data == null || data.length == 0 || objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        if (objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            contentType = "image/jpeg";
        }

        S3Client s3Client = buildS3Client();
        if (s3Client == null) {
            log.warn("AWS S3 client credentials or IAM instance role not available for upload to bucket [{}]. Storing in PostgreSQL with S3 reference.", bucketName);
            return null;
        }

        try (s3Client) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));
            String endpointUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectKey;
            log.info("Successfully uploaded object to AWS S3: {}", endpointUrl);
            return endpointUrl;
        } catch (Exception e) {
            log.warn("AWS S3 direct upload failed for object [{}] in bucket [{}]: {}. Storing in PostgreSQL with S3 reference.",
                    objectKey, bucketName, e.getMessage());
            return null;
        }
    }

    /**
     * Uploads file via presigned URL (Adopts team's HttpClient pattern).
     */
    public void uploadToS3(Path file, long contentLength, String presignedUrl, String contentType)
            throws IOException, InterruptedException {

        log.info(String.format("Uploading %d bytes (%.2f MB) to S3 via presigned URL",
                contentLength, contentLength / (1024.0 * 1024.0)));

        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(presignedUrl))
                .header("Content-Type", contentType != null ? contentType : "image/jpeg")
                .header("Content-Length", String.valueOf(contentLength))
                .PUT(HttpRequest.BodyPublishers.ofFile(file))
                .build();

        HttpResponse<String> uploadResponse = httpClient.send(
                uploadRequest, HttpResponse.BodyHandlers.ofString());

        if (uploadResponse.statusCode() < 200 || uploadResponse.statusCode() >= 300) {
            throw new IOException(String.format(
                    "S3 upload failed (HTTP %d): %s",
                    uploadResponse.statusCode(), uploadResponse.body()));
        }

        log.info("Document uploaded to S3 successfully");
    }

    /**
     * Downloads object bytes directly from AWS S3 using AWS SDK S3Client.
     */
    public byte[] getObjectBytes(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        if (objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }
        S3Client s3Client = buildS3Client();
        if (s3Client == null) {
            log.warn("AWS S3 client not available to retrieve object [{}] from bucket [{}]", objectKey, bucketName);
            return null;
        }
        try (s3Client) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return responseBytes.asByteArray();
        } catch (Exception e) {
            log.warn("Could not retrieve S3 object [{}] from bucket [{}]: {}", objectKey, bucketName, e.getMessage());
            return null;
        }
    }
}
