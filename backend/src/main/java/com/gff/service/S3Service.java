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

    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public S3Service() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Uploads in-memory byte array directly to AWS S3 bucket using AWS SigV4.
     *
     * @param data        Raw file bytes
     * @param objectKey   S3 key (e.g. "visiting-cards/REC-123.jpg")
     * @param contentType MIME type (e.g. "image/jpeg")
     * @return Public/Object URL of the uploaded S3 asset
     */
    public String uploadDirectToS3(byte[] data, String objectKey, String contentType) throws Exception {
        if (contentType == null || contentType.isEmpty()) {
            contentType = "image/jpeg";
        }

        // Clean leading slash
        if (objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }

        String host = bucketName + ".s3." + region + ".amazonaws.com";
        String endpointUrl = "https://" + host + "/" + objectKey;

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(AMZ_DATE_FORMAT);
        String dateStamp = now.format(DATE_STAMP_FORMAT);

        byte[] payloadHashBytes = sha256(data);
        String payloadHashHex = toHex(payloadHashBytes);

        String canonicalUri = "/" + objectKey;
        String canonicalHeaders = "content-type:" + contentType + "\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHashHex + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";

        String canonicalRequest = "PUT\n"
                + canonicalUri + "\n"
                + "\n" // query string
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHashHex;

        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";
        String stringToSign = algorithm + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + toHex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, "s3");
        String signature = toHex(hmacSha256(signingKey, stringToSign));

        String authHeader = algorithm + " "
                + "Credential=" + accessKey + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        log.info("Initiating AWS S3 PUT to {} (size: {} bytes)", endpointUrl, data.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", contentType)
                .header("x-amz-content-sha256", payloadHashHex)
                .header("x-amz-date", amzDate)
                .header("Authorization", authHeader)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("S3 upload failed with HTTP {}: {}", response.statusCode(), response.body());
            throw new IOException(String.format("AWS S3 upload failed (HTTP %d): %s",
                    response.statusCode(), response.body()));
        }

        log.info("Successfully uploaded object to AWS S3: {}", endpointUrl);
        return endpointUrl;
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

    private byte[] sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
