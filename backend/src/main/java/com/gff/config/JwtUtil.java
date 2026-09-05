package com.gff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight, zero-dependency JWT utility using HMAC-SHA256.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final String secretKey;
    private final long expirationMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUtil(
            @Value("${jwt.secret:qualtech-gff-secret-token-key-2026-very-secure}") String secretKey,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) { // 24 hours default
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, String name, String role) {
        try {
            long now = System.currentTimeMillis();
            long exp = now + expirationMs;

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", email);
            payload.put("userId", userId);
            payload.put("name", name);
            payload.put("role", role);
            payload.put("iat", now / 1000);
            payload.put("exp", exp / 1000);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String dataToSign = encodedHeader + "." + encodedPayload;

            String signature = sign(dataToSign, secretKey);
            return dataToSign + "." + signature;
        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Could not generate JWT token", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> validateAndExtractClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(dataToSign, secretKey);

            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Invalid JWT signature");
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            if (claims.containsKey("exp")) {
                long expSeconds = ((Number) claims.get("exp")).longValue();
                if (System.currentTimeMillis() / 1000 > expSeconds) {
                    log.warn("JWT token has expired");
                    return null;
                }
            }

            return claims;
        } catch (Exception e) {
            log.warn("Failed to validate JWT token: {}", e.getMessage());
            return null;
        }
    }

    private String sign(String data, String secret) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(hash);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
