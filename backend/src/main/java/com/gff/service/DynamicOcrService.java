package com.gff.service;

import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.repository.VisitingCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.S3Object;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamic OCR Service for Visiting Cards:
 * Performs real-time dynamic text extraction using AWS Textract or IRIS.
 * Parses names, designations, company names, emails, and phone numbers dynamically.
 * Zero hardcoded names or mock values.
 */
@Service
public class DynamicOcrService {

    private static final Logger log = LoggerFactory.getLogger(DynamicOcrService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d{1,3}[-\\s.]?)?\\(?\\d{3,5}\\)?[-\\s.]?\\d{3,5}[-\\s.]?\\d{3,5}");
    private static final Pattern DESIGNATION_PATTERN = Pattern.compile("(?i)\\b(software engineer|senior engineer|lead engineer|developer|architect|director|manager|vice president|vp|ceo|cto|cfo|founder|co-founder|consultant|analyst|specialist|officer|head|executive)\\b");
    private static final Pattern COMPANY_PATTERN = Pattern.compile("(?i)\\b(technologies|solutions|services|systems|infotech|pvt|ltd|limited|inc|corp|corporation|group|bank|fintech|labs|imgc|qualtech)\\b");

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.region:ap-south-1}")
    private String region;

    private final IrisService irisService;
    private final VisitingCardRepository visitingCardRepository;

    public DynamicOcrService(IrisService irisService, VisitingCardRepository visitingCardRepository) {
        this.irisService = irisService;
        this.visitingCardRepository = visitingCardRepository;
    }

    /**
     * Dynamically processes OCR for a visiting card record without any hardcoded values.
     *
     * @param card The visiting card entity
     * @return true if successfully extracted, false if failed/unreadable
     */
    public boolean processCardOcrDynamically(VisitingCard card) {
        // 1. If card already has dynamic cardholder name (e.g. sent from frontend capture), preserve it
        if (card.getCardHolderName() != null && !card.getCardHolderName().trim().isEmpty()) {
            card.setOcrStatus(OcrStatus.COMPLETED);
            card.setStatus(RecordStatus.VERIFIED);
            if (card.getOcrProcessedAt() == null) {
                card.setOcrProcessedAt(LocalDateTime.now());
            }
            visitingCardRepository.save(card);
            log.info("Card {} already has dynamic OCR data (holder: {}). Preserved successfully.",
                    card.getRecordId(), card.getCardHolderName());
            return true;
        }

        // 2. Try AWS Textract if S3 key is present
        if (card.getS3Key() != null && !card.getS3Key().trim().isEmpty() && hasAwsCredentials()) {
            try {
                boolean extracted = extractWithAwsTextract(card);
                if (extracted) {
                    card.setOcrStatus(OcrStatus.COMPLETED);
                    card.setStatus(RecordStatus.VERIFIED);
                    card.setOcrProcessedAt(LocalDateTime.now());
                    visitingCardRepository.save(card);
                    log.info("Successfully extracted dynamic OCR via AWS Textract for card {}: Holder={}",
                            card.getRecordId(), card.getCardHolderName());
                    return true;
                }
            } catch (Exception ex) {
                log.warn("AWS Textract dynamic extraction failed for {}: {}", card.getRecordId(), ex.getMessage());
            }
        }

        // 3. Try IRIS Service if image URL or base64 is available
        if (card.getImageUrl() != null && !card.getImageUrl().trim().isEmpty()) {
            try {
                boolean irisExtracted = irisService.extractVisitingCardSync(card, card.getImageUrl());
                if (irisExtracted && card.getCardHolderName() != null && !card.getCardHolderName().trim().isEmpty()) {
                    card.setOcrStatus(OcrStatus.COMPLETED);
                    card.setStatus(RecordStatus.VERIFIED);
                    card.setOcrProcessedAt(LocalDateTime.now());
                    visitingCardRepository.save(card);
                    log.info("Successfully extracted dynamic OCR via IRIS for card {}", card.getRecordId());
                    return true;
                }
            } catch (Exception ex) {
                log.warn("IRIS dynamic extraction failed for {}: {}", card.getRecordId(), ex.getMessage());
            }
        }

        // 4. Try parsing existing raw OCR text dynamically if present
        if (card.getRawOcrText() != null && !card.getRawOcrText().trim().isEmpty()) {
            parseTextDynamically(card, card.getRawOcrText());
            if (card.getCardHolderName() != null && !card.getCardHolderName().trim().isEmpty()) {
                card.setOcrStatus(OcrStatus.COMPLETED);
                card.setStatus(RecordStatus.VERIFIED);
                card.setOcrProcessedAt(LocalDateTime.now());
                visitingCardRepository.save(card);
                log.info("Successfully parsed dynamic OCR from existing raw text for card {}", card.getRecordId());
                return true;
            }
        }

        // 5. If no dynamic data could be extracted, mark as FAILED (NEVER inject fake/hardcoded data)
        log.warn("No readable text could be extracted dynamically for card {}. Setting OCR status to FAILED.",
                card.getRecordId());
        card.setOcrStatus(OcrStatus.FAILED);
        card.setErrorMessage("Dynamic OCR extraction failed: document image unreadable or OCR service unreachable");
        card.setOcrProcessedAt(LocalDateTime.now());
        visitingCardRepository.save(card);
        return false;
    }

    private boolean extractWithAwsTextract(VisitingCard card) {
        String bucket = card.getS3Bucket() != null ? card.getS3Bucket() : "visiting-card-bkt";
        String key = card.getS3Key();

        TextractClient textractClient = TextractClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        try {
            DetectDocumentTextRequest textractRequest = DetectDocumentTextRequest.builder()
                    .document(Document.builder()
                            .s3Object(S3Object.builder()
                                    .bucket(bucket)
                                    .name(key)
                                    .build())
                            .build())
                    .build();

            DetectDocumentTextResponse textractResponse = textractClient.detectDocumentText(textractRequest);
            List<String> lines = new ArrayList<>();
            for (Block block : textractResponse.blocks()) {
                if (block.blockType() == BlockType.LINE && block.text() != null) {
                    lines.add(block.text().trim());
                }
            }

            if (lines.isEmpty()) {
                return false;
            }

            String fullText = String.join("\n", lines);
            card.setRawOcrText(fullText);
            parseTextDynamically(card, fullText);
            return card.getCardHolderName() != null && !card.getCardHolderName().trim().isEmpty();

        } finally {
            textractClient.close();
        }
    }

    public void parseTextDynamically(VisitingCard card, String fullText) {
        String[] lines = fullText.split("\\r?\\n");
        List<String> potentialNames = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // Extract Email
            Matcher emailMatcher = EMAIL_PATTERN.matcher(line);
            if (emailMatcher.find() && card.getExtractedEmail() == null) {
                card.setExtractedEmail(emailMatcher.group());
                continue;
            }

            // Extract Phone / Mobile
            Matcher phoneMatcher = PHONE_PATTERN.matcher(line);
            if (phoneMatcher.find() && card.getExtractedMobile() == null && line.replaceAll("[^0-9]", "").length() >= 10) {
                card.setExtractedMobile(phoneMatcher.group());
                continue;
            }

            // Extract Designation
            Matcher desigMatcher = DESIGNATION_PATTERN.matcher(line);
            if (desigMatcher.find() && card.getDesignation() == null) {
                card.setDesignation(line);
                continue;
            }

            // Extract Company Name
            Matcher compMatcher = COMPANY_PATTERN.matcher(line);
            if (compMatcher.find() && card.getCompanyName() == null) {
                card.setCompanyName(line);
                continue;
            }

            // Potential Person Name
            if (isPotentialPersonName(line)) {
                potentialNames.add(line);
            }
        }

        // Dynamically assign card holder name from the topmost valid name line
        if (card.getCardHolderName() == null && !potentialNames.isEmpty()) {
            card.setCardHolderName(potentialNames.get(0));
        }
    }

    private boolean isPotentialPersonName(String line) {
        if (line.length() < 3 || line.length() > 35) return false;
        if (line.matches(".*\\d.*")) return false; // Contains digits
        if (line.toLowerCase().contains("www.") || line.toLowerCase().contains(".com")) return false;
        if (line.toLowerCase().contains("phone") || line.toLowerCase().contains("email") || line.toLowerCase().contains("address")) return false;
        String[] words = line.split("\\s+");
        return words.length >= 1 && words.length <= 4;
    }

    private boolean hasAwsCredentials() {
        return accessKey != null && !accessKey.trim().isEmpty() &&
               secretKey != null && !secretKey.trim().isEmpty();
    }
}
