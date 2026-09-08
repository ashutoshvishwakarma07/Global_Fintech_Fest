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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamic OCR Service for Visiting Cards:
 * Performs real-time dynamic text extraction using AWS Textract or IRIS.
 * Parses all 14 standardized fields dynamically with zero hardcoded mock values.
 */
@Service
public class DynamicOcrService {

    private static final Logger log = LoggerFactory.getLogger(DynamicOcrService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d{1,3}[-\\s.]?)?\\(?\\d{2,5}\\)?[-\\s.]?\\d{3,5}[-\\s.]?\\d{3,5}");
    private static final Pattern WEB_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s/$.?#].[^\\s]*\\b");
    private static final Pattern PIN_PATTERN = Pattern.compile("(?i)\\b(?:PIN|PINCODE|ZIP|POSTAL)?\\s*(\\d{6}|\\d{5})\\b");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?linkedin\\.com/(?:in/)?([a-zA-Z0-9_-]+)");
    private static final Pattern TWITTER_PATTERN = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?(?:twitter\\.com|x\\.com)/([a-zA-Z0-9_]+)");

    private static final Pattern DESIGNATION_PATTERN = Pattern.compile("(?i)\\b(software engineer|senior engineer|lead engineer|developer|architect|director|manager|vice president|vp|ceo|cto|cfo|coo|founder|co-founder|consultant|analyst|specialist|officer|head|president|executive|managing director|general manager|associate|partner|principal)\\b");
    private static final Pattern COMPANY_PATTERN = Pattern.compile("(?i)\\b(technologies|solutions|services|systems|infotech|pvt|ltd|limited|inc|corp|corporation|group|bank|fintech|labs|qualtech|capital|enterprises|consulting|software)\\b");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("(?i)\\b(human resources|hr|sales|marketing|engineering|operations|finance|information technology|it|legal|research & development|r&d|customer support|support|accounts|procurement|product|administration|admin|business development|quality assurance|qa)\\b");

    private static final List<String> INDIAN_STATES = Arrays.asList(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
            "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
            "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
            "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
            "Delhi", "NCT of Delhi", "Chandigarh", "Puducherry"
    );

    private static final List<String> MAJOR_CITIES = Arrays.asList(
            "Mumbai", "Delhi", "Bengaluru", "Bangalore", "Hyderabad", "Ahmedabad", "Chennai",
            "Kolkata", "Surat", "Pune", "Jaipur", "Lucknow", "Kanpur", "Nagpur", "Indore",
            "Thane", "Bhopal", "Visakhapatnam", "Pimpri-Chinchwad", "Patna", "Vadodara",
            "Ghaziabad", "Ludhiana", "Agra", "Nashik", "Faridabad", "Meerut", "Rajkot",
            "Kalyan-Dombivli", "Vasai-Virar", "Varanasi", "Srinagar", "Aurangabad", "Dhanbad",
            "Amritsar", "Navi Mumbai", "Allahabad", "Prayagraj", "Ranchi", "Howrah", "Coimbatore",
            "Jabalpur", "Gwalior", "Vijayawada", "Jodhpur", "Madurai", "Raipur", "Kota", "Guwahati",
            "Chandigarh", "Solapur", "Hubballi-Dharwad", "Bareilly", "Moradabad", "Mysore", "Gurgaon",
            "Gurugram", "Noida", "Greater Noida", "New York", "London", "Dubai", "Singapore", "San Francisco"
    );

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
        if (card.getS3Key() != null && !card.getS3Key().trim().isEmpty()) {
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

        // 5. If no dynamic data could be extracted, mark as FAILED
        log.warn("No readable text could be extracted dynamically for card {}. Setting OCR status to FAILED.",
                card.getRecordId());
        card.setOcrStatus(OcrStatus.FAILED);
        card.setErrorMessage("Dynamic OCR extraction failed: document image unreadable or OCR service unreachable");
        card.setOcrProcessedAt(LocalDateTime.now());
        visitingCardRepository.save(card);
        return false;
    }

    public boolean hasValidCredentials() {
        return accessKey != null && !accessKey.trim().isEmpty() &&
               secretKey != null && !secretKey.trim().isEmpty();
    }

    private TextractClient buildTextractClient() {
        try {
            if (hasValidCredentials()) {
                return TextractClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey.trim(), secretKey.trim())
                        ))
                        .build();
            } else {
                try {
                    DefaultCredentialsProvider provider = DefaultCredentialsProvider.create();
                    provider.resolveCredentials();
                    return TextractClient.builder()
                            .region(Region.of(region))
                            .credentialsProvider(provider)
                            .build();
                } catch (Exception ex) {
                    log.debug("DefaultCredentialsProvider not available for Textract: {}", ex.getMessage());
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build TextractClient: {}", e.getMessage());
            return null;
        }
    }

    private boolean extractWithAwsTextract(VisitingCard card) {
        String bucket = card.getS3Bucket() != null ? card.getS3Bucket() : "visiting-card-bkt";
        String key = card.getS3Key();

        TextractClient textractClient = buildTextractClient();
        if (textractClient == null) {
            log.debug("Textract client not available for dynamic extraction on card {}", card.getRecordId());
            return false;
        }

        try (textractClient) {
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

        } catch (Exception e) {
            log.warn("AWS Textract execution error on card {}: {}", card.getRecordId(), e.getMessage());
            return false;
        }
    }

    public void parseTextDynamically(VisitingCard card, String fullText) {
        String[] lines = fullText.split("\\r?\\n");
        List<String> potentialNames = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase();

            // 1. LinkedIn
            if (card.getLinkedIn() == null && (lower.contains("linkedin.com") || lower.startsWith("linkedin:"))) {
                Matcher liMatch = LINKEDIN_PATTERN.matcher(line);
                if (liMatch.find()) {
                    card.setLinkedIn(liMatch.group());
                } else {
                    card.setLinkedIn(line.replaceFirst("(?i)^linkedin[:\\s-]*", "").trim());
                }
                continue;
            }

            // 2. Twitter / X
            if (card.getTwitter() == null && (lower.contains("twitter.com") || lower.contains("x.com") || lower.startsWith("twitter:") || lower.startsWith("x:"))) {
                Matcher twMatch = TWITTER_PATTERN.matcher(line);
                if (twMatch.find()) {
                    card.setTwitter(twMatch.group());
                } else {
                    card.setTwitter(line.replaceFirst("(?i)^(?:twitter|x)[:\\s-]*", "").trim());
                }
                continue;
            }

            // 3. Email Address
            Matcher emailMatcher = EMAIL_PATTERN.matcher(line);
            if (emailMatcher.find() && card.getExtractedEmail() == null) {
                card.setExtractedEmail(emailMatcher.group());
                continue;
            }

            // 4. Website URL
            if (card.getWebsiteUrl() == null && !lower.contains("linkedin") && !lower.contains("twitter")) {
                Matcher webMatcher = WEB_PATTERN.matcher(line);
                if (webMatcher.find()) {
                    card.setWebsiteUrl(webMatcher.group().replaceAll("[^\\w./-]", ""));
                    continue;
                }
            }

            // 5. Work Number vs Mobile Number
            if (lower.contains("work") || lower.contains("tel") || lower.contains("office") || lower.contains("off:") || lower.contains("landline")) {
                Matcher phoneMatcher = PHONE_PATTERN.matcher(line);
                if (phoneMatcher.find() && card.getWorkNumber() == null) {
                    card.setWorkNumber(phoneMatcher.group().trim());
                    continue;
                }
            }

            Matcher phoneMatcher = PHONE_PATTERN.matcher(line);
            if (phoneMatcher.find()) {
                String matchedPhone = phoneMatcher.group().trim();
                if (card.getExtractedMobile() == null && line.replaceAll("[^0-9]", "").length() >= 10) {
                    card.setExtractedMobile(matchedPhone);
                    continue;
                } else if (card.getWorkNumber() == null && !matchedPhone.equals(card.getExtractedMobile())) {
                    card.setWorkNumber(matchedPhone);
                    continue;
                }
            }

            // 6. Department
            if (card.getDepartment() == null) {
                Matcher deptMatcher = DEPARTMENT_PATTERN.matcher(line);
                if (deptMatcher.find()) {
                    card.setDepartment(deptMatcher.group());
                    continue;
                }
            }

            // 7. Designation / Job Title
            Matcher desigMatcher = DESIGNATION_PATTERN.matcher(line);
            if (desigMatcher.find() && card.getDesignation() == null) {
                card.setDesignation(line.replaceFirst("(?i)^(?:designation|title|role)[:\\s-]*", "").trim());
                continue;
            }

            // 8. Company Name
            Matcher compMatcher = COMPANY_PATTERN.matcher(line);
            if (compMatcher.find() && card.getCompanyName() == null) {
                card.setCompanyName(line.replaceFirst("(?i)^(?:company|org|organization)[:\\s-]*", "").trim());
                continue;
            }

            // 9. Postal / ZIP Code
            if (card.getPostalZipCode() == null) {
                Matcher pinMatcher = PIN_PATTERN.matcher(line);
                if (pinMatcher.find()) {
                    card.setPostalZipCode(pinMatcher.group(1));
                }
            }

            // 10. City
            if (card.getCity() == null) {
                for (String c : MAJOR_CITIES) {
                    if (Pattern.compile("(?i)\\b" + Pattern.quote(c) + "\\b").matcher(line).find()) {
                        card.setCity(c);
                        break;
                    }
                }
            }

            // 11. State
            if (card.getState() == null) {
                for (String s : INDIAN_STATES) {
                    if (Pattern.compile("(?i)\\b" + Pattern.quote(s) + "\\b").matcher(line).find()) {
                        card.setState(s);
                        break;
                    }
                }
            }

            // 12. Country
            if (card.getCountry() == null) {
                Matcher countryMatcher = Pattern.compile("(?i)\\b(India|USA|United States|UK|United Kingdom|UAE|Singapore|Australia|Germany|Canada)\\b").matcher(line);
                if (countryMatcher.find()) {
                    card.setCountry(countryMatcher.group());
                }
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

        // Default country if state or known city is found
        if (card.getCountry() == null && (card.getState() != null || (card.getCity() != null && MAJOR_CITIES.subList(0, 50).contains(card.getCity())))) {
            card.setCountry("India");
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
}
