package com.gff.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.repository.VisitingCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for integrating with IRIS Document Intelligence API
 * as specified in the IRIS External API Guide v1.0.
 */
@Service
public class IrisService {

    private static final Logger log = LoggerFactory.getLogger(IrisService.class);

    @Value("${iris.base-url:http://localhost:5000}")
    private String irisBaseUrl;

    @Value("${iris.application-id:APP001}")
    private String applicationId;

    @Value("${iris.password:secret}")
    private String password;

    private String cachedAccessToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final VisitingCardRepository visitingCardRepository;

    public IrisService(VisitingCardRepository visitingCardRepository) {
        this.visitingCardRepository = visitingCardRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Authenticates with IRIS to obtain JWT access token.
     * POST /login
     */
    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && !cachedAccessToken.isEmpty()) {
            return cachedAccessToken;
        }

        try {
            Map<String, String> loginBody = Map.of(
                    "applicationId", applicationId,
                    "password", password
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(irisBaseUrl + "/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                if (json.has("accessToken")) {
                    this.cachedAccessToken = json.get("accessToken").asText();
                    log.info("Successfully authenticated with IRIS API");
                    return this.cachedAccessToken;
                }
            }
            log.warn("IRIS login response status {}: {}", response.statusCode(), response.body());
        } catch (Exception e) {
            log.error("Failed to authenticate with IRIS API: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Sync extraction for a visiting card.
     * POST /extract-document
     */
    public boolean extractVisitingCardSync(VisitingCard card, String base64Image) {
        try {
            String token = getAccessToken();
            if (token == null) {
                log.warn("Cannot extract: IRIS authentication token is null");
                return false;
            }

            Map<String, Object> payload = Map.of(
                    "appID", applicationId,
                    "entityType", "applicant",
                    "entityRef", card.getUploaderEmail(),
                    "documentName", "Visiting Card",
                    "documentRef", card.getRecordId(),
                    "userID", card.getUploaderEmail(),
                    "files", List.of(Map.of(
                            "fileObjectRef", card.getFileName() != null ? card.getFileName() : "card.jpg",
                            "fileObject", base64Image
                    ))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(irisBaseUrl + "/extract-document"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                if ("S".equalsIgnoreCase(json.path("status").asText())) {
                    parseAndSaveOcrResult(card, json);
                    return true;
                }
            }
            log.warn("IRIS extract-document failed: {}", response.body());
        } catch (Exception e) {
            log.error("Error during IRIS sync extraction for record {}: {}", card.getRecordId(), e.getMessage());
        }
        return false;
    }

    private void parseAndSaveOcrResult(VisitingCard card, JsonNode json) {
        card.setOcrStatus(OcrStatus.COMPLETED);
        card.setRawOcrText(json.path("raw_text").asText());

        JsonNode kv = json.path("fileObjectJSON").path("key_value_pairs");
        if (!kv.isMissingNode()) {
            if (kv.has("name")) card.setCardHolderName(kv.get("name").asText());
            if (kv.has("company")) card.setCompanyName(kv.get("company").asText());
            if (kv.has("designation")) card.setDesignation(kv.get("designation").asText());
            if (kv.has("email")) card.setExtractedEmail(kv.get("email").asText());
            if (kv.has("phone") || kv.has("mobile")) {
                card.setExtractedMobile(kv.has("mobile") ? kv.get("mobile").asText() : kv.get("phone").asText());
            }
            if (kv.has("address")) card.setExtractedAddress(kv.get("address").asText());
        }
        card.setOcrProcessedAt(LocalDateTime.now());
        visitingCardRepository.save(card);
        log.info("Successfully updated visiting card {} with IRIS OCR extraction", card.getRecordId());
    }
}
