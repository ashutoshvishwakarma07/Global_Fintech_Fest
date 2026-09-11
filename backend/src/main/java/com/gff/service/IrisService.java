package com.gff.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for integrating with IRIS Document Intelligence API
 * as specified in the IRIS External API Guide v1.0.
 */
@Service
public class IrisService {

    private static final Logger log = LoggerFactory.getLogger(IrisService.class);

    @Value("${iris.base-url:http://iris-qa.qualtechedge.in/python}")
    private String irisBaseUrl;

    @Value("${iris.extract-endpoint:/extract-document-public}")
    private String extractEndpoint;

    @Value("${iris.application-id:42b9fd86-8f6d-403e-93a1-3a5856a7665e}")
    private String applicationId;

    @Value("${iris.password:password1}")
    private String password;

    @Value("${iris.entity-ref:ENT-1001}")
    private String entityRef;

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
                    log.info("Successfully authenticated with IRIS API at {}/login", irisBaseUrl);
                    return this.cachedAccessToken;
                }
            }
            log.warn("IRIS login response status {} from [{}/login]: {}", response.statusCode(), irisBaseUrl, response.body());
        } catch (Exception e) {
            log.error("Failed to authenticate with IRIS API at [{}/login]: {} (Cause: {})",
                    irisBaseUrl, e.getClass().getSimpleName(), (e.getMessage() != null ? e.getMessage() : e.getCause()));
        }
        return null;
    }

    /**
     * Sync extraction for a visiting card.
     * POST /extract-document-public (or /extract-document)
     */
    public boolean extractVisitingCardSync(VisitingCard card, String base64Image) {
        try {
            String ref = (entityRef != null && !entityRef.trim().isEmpty()) ? entityRef.trim() : "ENT-1001";

            Map<String, Object> payload = Map.of(
                    "appID", applicationId != null ? applicationId : "42b9fd86-8f6d-403e-93a1-3a5856a7665e",
                    "entityType", "applicant",
                    "entityRef", ref,
                    "documentName", "Visiting Card",
                    "documentRef", card.getRecordId(),
                    "userID", card.getUploaderEmail(),
                    "files", List.of(Map.of(
                            "fileObjectRef", card.getFileName() != null ? card.getFileName() : "visiting_card.jpg",
                            "fileObject", base64Image
                    ))
            );

            String requestJson = objectMapper.writeValueAsString(payload);
            String ep = (extractEndpoint != null && !extractEndpoint.trim().isEmpty()) ? extractEndpoint.trim() : "/extract-document-public";
            if (!ep.startsWith("/")) ep = "/" + ep;
            String fullUrl = irisBaseUrl + ep;

            log.info("IRIS OCR extraction Request Payload for record [{}] to [{}]:\nappID={}, entityRef={}, docRef={}, fileRef={}",
                    card.getRecordId(), fullUrl, applicationId, ref, card.getRecordId(), card.getFileName());

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson));

            // Attach token if available (for protected endpoints)
            try {
                String token = getAccessToken();
                if (token != null && !token.trim().isEmpty()) {
                    reqBuilder.header("Authorization", "Bearer " + token);
                }
            } catch (Exception tErr) {
                log.debug("IRIS token fetch skipped/failed, proceeding with public extraction: {}", tErr.getMessage());
            }

            HttpRequest request = reqBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(responseBody);
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                log.info("IRIS OCR extraction Response JSON for record [{}]:\n{}", card.getRecordId(), prettyJson);

                if ("S".equalsIgnoreCase(json.path("status").asText())) {
                    parseAndSaveOcrResult(card, json);
                    return true;
                } else {
                    log.warn("IRIS OCR extraction returned non-success status [{}] for record [{}]:\n{}",
                            json.path("status").asText(), card.getRecordId(), prettyJson);
                }
            } else {
                log.warn("IRIS OCR extraction failed with HTTP status {} for record [{}]:\n{}",
                        response.statusCode(), card.getRecordId(), responseBody);
            }
        } catch (Exception e) {
            log.error("Error during IRIS sync extraction for record {}: {}", card.getRecordId(), e.getMessage(), e);
        }
        return false;
    }

    private void parseAndSaveOcrResult(VisitingCard card, JsonNode json) {
        card.setOcrStatus(OcrStatus.COMPLETED);
        card.setStatus(RecordStatus.VERIFIED);
        if (json.has("raw_text") && !json.get("raw_text").isNull()) {
            card.setRawOcrText(json.get("raw_text").asText());
        }

        Map<String, String> extractedMap = new HashMap<>();

        // 1. Extract from nested_json (e.g. "Name", "job_title", "company_name", "department", "email_address", "mobile_number", etc.)
        JsonNode nested = json.path("fileObjectJSON").path("nested_json");
        if (nested.isObject()) {
            nested.fields().forEachRemaining(entry -> {
                if (entry.getValue() != null && !entry.getValue().isNull()) {
                    String val = entry.getValue().asText().trim();
                    if (!val.isEmpty()) {
                        extractedMap.put(entry.getKey().toLowerCase().replace("_", "").replace(" ", ""), val);
                    }
                }
            });
        }

        // 2. Extract from key_value_pairs groups (e.g. Others: [{ key: "Name", value: "..." }], PersonalInfo: [...])
        JsonNode kv = json.path("fileObjectJSON").path("key_value_pairs");
        if (kv.isObject()) {
            kv.fields().forEachRemaining(category -> {
                JsonNode items = category.getValue();
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        if (item.has("key") && item.has("value") && !item.get("value").isNull()) {
                            String k = item.get("key").asText().toLowerCase().replace("_", "").replace(" ", "");
                            String v = item.get("value").asText().trim();
                            if (!v.isEmpty()) {
                                extractedMap.put(k, v);
                            }
                        }
                    }
                } else if (items.isTextual() && !items.asText().trim().isEmpty()) {
                    extractedMap.put(category.getKey().toLowerCase().replace("_", "").replace(" ", ""), items.asText().trim());
                }
            });
        }

        // Helper to find by candidate keys
        Function<String[], String> findField = (keys) -> {
            for (String k : keys) {
                String normalized = k.toLowerCase().replace("_", "").replace(" ", "");
                if (extractedMap.containsKey(normalized)) {
                    return extractedMap.get(normalized);
                }
            }
            return null;
        };

        // 1. Name / Card Holder Name
        String name = findField.apply(new String[]{"name", "cardholdername", "holdername", "fullname"});
        if (name != null) card.setCardHolderName(name);

        // 2. Job Title / Designation
        String designation = findField.apply(new String[]{"jobtitle", "designation", "title", "role", "position"});
        if (designation != null) card.setDesignation(designation);

        // 3. Company Name
        String company = findField.apply(new String[]{"companyname", "company", "organization", "firm"});
        if (company != null) card.setCompanyName(company);

        // 4. Department
        String department = findField.apply(new String[]{"department", "dept", "division", "team"});
        if (department != null) card.setDepartment(department);

        // 5. Email Address
        String email = findField.apply(new String[]{"emailaddress", "email", "mail", "contactemail"});
        if (email != null) card.setExtractedEmail(email);

        // 6. Mobile Number
        String mobile = findField.apply(new String[]{"mobilenumber", "mobile", "phone", "phonenumber", "cell"});
        if (mobile != null) card.setExtractedMobile(mobile);

        // 7. Work Number
        String workNum = findField.apply(new String[]{"worknumber", "workphone", "telephone", "officephone", "landline", "officenumber"});
        if (workNum != null) card.setWorkNumber(workNum);

        // 8. Website URL
        String website = findField.apply(new String[]{"websiteurl", "website", "url", "web", "site"});
        if (website != null) card.setWebsiteUrl(website);

        // 9. Address
        String address = findField.apply(new String[]{"address", "extractedaddress", "officeaddress", "location", "streetaddress"});
        if (address != null) card.setExtractedAddress(address);

        // 10. City
        String city = findField.apply(new String[]{"city", "town"});
        if (city != null) card.setCity(city);

        // 11. State
        String state = findField.apply(new String[]{"state", "province", "region"});
        if (state != null) card.setState(state);

        // 12. Postal / ZIP Code
        String postal = findField.apply(new String[]{"postalcode", "pincode", "postalzipcode", "zip", "zipcode"});
        if (postal != null) card.setPostalZipCode(postal);

        // 13. Country
        String country = findField.apply(new String[]{"country", "nation"});
        if (country != null) card.setCountry(country);

        // 14. LinkedIn
        String linkedin = findField.apply(new String[]{"linkedin", "linkedinurl", "profile"});
        if (linkedin != null) card.setLinkedIn(linkedin);

        // 15. Twitter / X
        String twitter = findField.apply(new String[]{"twitter", "x", "twitterhandle"});
        if (twitter != null) card.setTwitter(twitter);

        card.setOcrProcessedAt(LocalDateTime.now());
        visitingCardRepository.save(card);
        log.info("Successfully updated visiting card [{}] with IRIS OCR extraction: Name={}, Company={}, Designation={}, Mobile={}, Email={}",
                card.getRecordId(), card.getCardHolderName(), card.getCompanyName(), card.getDesignation(), card.getExtractedMobile(), card.getExtractedEmail());
    }
}
