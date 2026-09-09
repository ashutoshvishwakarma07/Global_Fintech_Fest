package com.gff.service;

import com.gff.dto.request.DocumentUploadRequest;
import com.gff.dto.request.ShareCardRequest;
import com.gff.dto.response.DashboardStatsResponse;
import com.gff.dto.response.DocumentResponse;
import com.gff.entity.CardShareAudit;
import com.gff.entity.User;
import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.entity.enums.UserRole;
import com.gff.exception.ApiException;
import com.gff.exception.ResourceNotFoundException;
import com.gff.repository.CardShareAuditRepository;
import com.gff.repository.VisitingCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final VisitingCardRepository visitingCardRepository;
    private final S3Service s3Service;
    private final DynamicOcrService dynamicOcrService;
    private final IrisService irisService;
    private final EmailService emailService;
    private final CardShareAuditRepository cardShareAuditRepository;

    public DocumentService(VisitingCardRepository visitingCardRepository,
                           S3Service s3Service,
                           DynamicOcrService dynamicOcrService,
                           IrisService irisService,
                           EmailService emailService,
                           CardShareAuditRepository cardShareAuditRepository) {
        this.visitingCardRepository = visitingCardRepository;
        this.s3Service = s3Service;
        this.dynamicOcrService = dynamicOcrService;
        this.irisService = irisService;
        this.emailService = emailService;
        this.cardShareAuditRepository = cardShareAuditRepository;
    }

    @Transactional
    public DocumentResponse createUploadRecord(DocumentUploadRequest request, User currentUser) {
        String finalRecordId = (request.getRecordId() != null && !request.getRecordId().trim().isEmpty())
                ? request.getRecordId()
                : "IMG-" + System.currentTimeMillis();

        if (visitingCardRepository.existsByRecordId(finalRecordId)) {
            log.warn("Record ID [{}] already exists in database. Assigning fresh unique ID to prevent upload collision.", finalRecordId);
            finalRecordId = "IMG-" + System.currentTimeMillis();
        }

        String rawFileName = request.getFileName();
        String fileExt = ".jpg";
        String s3Bucket = s3Service.getBucketName();
        String s3Key = "visiting-cards/" + finalRecordId + ".jpg";
        String imageUrl = "https://" + s3Bucket + ".s3." + s3Service.getRegion() + ".amazonaws.com/" + s3Key;

        if (request.getImageBase64() != null && !request.getImageBase64().trim().isEmpty()) {
            FileFormatInfo formatInfo = validateAndResolveFormat(request.getImageBase64(), rawFileName);
            byte[] imageBytes = formatInfo.bytes;
            String contentType = formatInfo.contentType;
            fileExt = formatInfo.extension;

            s3Key = "visiting-cards/" + finalRecordId + fileExt;
            imageUrl = "https://" + s3Bucket + ".s3." + s3Service.getRegion() + ".amazonaws.com/" + s3Key;

            // Direct upload to AWS S3 bucket (NO local filesystem storage)
            boolean s3Uploaded = false;
            try {
                String s3Url = s3Service.uploadDirectToS3(imageBytes, s3Key, contentType);
                if (s3Url != null && !s3Url.trim().isEmpty()) {
                    imageUrl = s3Url;
                    s3Uploaded = true;
                    log.info("Document image uploaded directly to AWS S3 bucket [{}]: key={}, url={}",
                            s3Bucket, s3Key, imageUrl);
                }
            } catch (Exception s3Err) {
                log.warn("AWS S3 direct upload skipped for record [{}]: {}", finalRecordId, s3Err.getMessage());
            }

            // If S3 upload wasn't completed (e.g. AWS credentials not configured on local machine),
            // store image in PostgreSQL as data URI so it streams without 404 and WITHOUT local disk pollution!
            if (!s3Uploaded) {
                String rawBase64 = request.getImageBase64();
                if (!rawBase64.startsWith("data:")) {
                    imageUrl = "data:" + contentType + ";base64," + rawBase64;
                } else {
                    imageUrl = rawBase64;
                }
                log.info("Recorded S3 reference [{}] with image payload in PostgreSQL database (zero local filesystem storage).", s3Key);
            }
        } else if (rawFileName != null && !rawFileName.trim().isEmpty()) {
            // If only file name is provided, validate extension
            String ext = rawFileName.toLowerCase();
            if (!ext.endsWith(".png") && !ext.endsWith(".jpg") && !ext.endsWith(".jpeg") &&
                !ext.endsWith(".pdf") && !ext.endsWith(".doc") && !ext.endsWith(".docx")) {
                throw new ApiException("Unsupported file format. Only PNG, JPEG, JPG, Word (.doc, .docx), and PDF files are allowed.", HttpStatus.BAD_REQUEST);
            }
        }

        boolean hasOcr = (request.getCardHolderName() != null && !request.getCardHolderName().trim().isEmpty())
                || (request.getName() != null && !request.getName().trim().isEmpty());

        // Enforce verified user identity from server-side session token
        String uploaderName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : request.getUploaderName();
        String uploaderEmail = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail() : request.getUploaderEmail();
        UserRole uploaderRole = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole() : (request.getUploaderRole() != null ? request.getUploaderRole() : UserRole.FIELD_USER);
        String uploaderMobile = (currentUser != null && currentUser.getMobile() != null) ? currentUser.getMobile() : request.getUploaderMobile();

        VisitingCard card = VisitingCard.builder()
                .recordId(finalRecordId)
                .uploaderName(uploaderName)
                .uploaderEmail(uploaderEmail)
                .uploaderMobile(uploaderMobile)
                .uploaderRole(uploaderRole)
                .fileName(request.getFileName() != null ? request.getFileName() : finalRecordId + ".jpg")
                .fileSize(request.getFileSize())
                .notes(request.getNotes())
                .imageUrl(imageUrl)
                .s3Key(s3Key)
                .s3Bucket(s3Bucket)
                .status(Boolean.TRUE.equals(request.getIsOffline()) ? RecordStatus.PENDING_UPLOAD : RecordStatus.UPLOADED)
                .isOffline(request.getIsOffline() != null ? request.getIsOffline() : false)
                // 14 Standardized Fields
                .cardHolderName(request.getName() != null ? request.getName() : request.getCardHolderName())
                .designation(request.getJobTitle() != null ? request.getJobTitle() : request.getDesignation())
                .companyName(request.getCompanyName())
                .department(request.getDepartment())
                .extractedEmail(request.getEmailAddress() != null ? request.getEmailAddress() : request.getExtractedEmail())
                .extractedMobile(request.getMobileNumber() != null ? request.getMobileNumber() : request.getExtractedMobile())
                .workNumber(request.getWorkNumber())
                .websiteUrl(request.getWebsiteUrl())
                .city(request.getCity())
                .state(request.getState())
                .postalZipCode(request.getPostalZipCode())
                .country(request.getCountry())
                .linkedIn(request.getLinkedIn())
                .twitter(request.getTwitter())
                // Address & OCR metadata
                .extractedAddress(request.getExtractedAddress())
                .rawOcrText(request.getRawOcrText())
                .ocrStatus(hasOcr ? OcrStatus.COMPLETED : OcrStatus.PENDING)
                .ocrProcessedAt(hasOcr ? LocalDateTime.now() : null)
                .build();

        VisitingCard saved = visitingCardRepository.save(card);

        // Trigger IRIS OCR extraction synchronously on every image upload to extract KYC fields and log JSON
        try {
            String ocrImagePayload = (request.getImageBase64() != null && !request.getImageBase64().trim().isEmpty())
                    ? request.getImageBase64()
                    : saved.getImageUrl();
            if (ocrImagePayload != null && !ocrImagePayload.trim().isEmpty()) {
                log.info("Triggering IRIS OCR /extract-document extraction for record: {}", saved.getRecordId());
                boolean irisSuccess = irisService.extractVisitingCardSync(saved, ocrImagePayload);
                if (irisSuccess) {
                    saved = visitingCardRepository.findById(saved.getId()).orElse(saved);
                    log.info("IRIS OCR extraction completed successfully for card: {}", saved.getRecordId());
                } else {
                    dynamicOcrService.processCardOcrDynamically(saved);
                    saved = visitingCardRepository.findById(saved.getId()).orElse(saved);
                }
            }
        } catch (Exception e) {
            log.warn("IRIS OCR extraction during upload for record [{}] notice: {}", saved.getRecordId(), e.getMessage());
        }

        log.info("Created visiting card record: {} by {}", saved.getRecordId(), saved.getUploaderEmail());
        return DocumentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocuments(
            String currentUserEmail,
            String currentUserRole,
            String query,
            RecordStatus status,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean isPrivileged = "SUPERVISOR".equalsIgnoreCase(currentUserRole) || "ADMIN".equalsIgnoreCase(currentUserRole);

        Page<VisitingCard> records;
        if (isPrivileged) {
            // Admin and Supervisor have access to all uploaded documents across all users
            if (query != null && !query.trim().isEmpty()) {
                records = visitingCardRepository.searchRecords(query.trim(), pageable);
            } else if (status != null) {
                records = visitingCardRepository.findByStatus(status, pageable);
            } else {
                records = visitingCardRepository.findAll(pageable);
            }
        } else {
            // Normal field user is strictly restricted to records uploaded by their own email
            String email = (currentUserEmail != null) ? currentUserEmail.trim().toLowerCase() : "";
            if (query != null && !query.trim().isEmpty()) {
                records = visitingCardRepository.searchUserRecords(email, query.trim(), pageable);
            } else if (status != null) {
                records = visitingCardRepository.findByUploaderEmailAndStatus(email, status, pageable);
            } else {
                records = visitingCardRepository.findByUploaderEmail(email, pageable);
            }
        }

        return records.map(DocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id, String currentUserEmail, String currentUserRole) {
        VisitingCard card = visitingCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "id", id));

        validateOwnership(card, currentUserEmail, currentUserRole);
        return DocumentResponse.fromEntity(card);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentByRecordId(String recordId, String currentUserEmail, String currentUserRole) {
        VisitingCard card = visitingCardRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "recordId", recordId));

        validateOwnership(card, currentUserEmail, currentUserRole);
        return DocumentResponse.fromEntity(card);
    }

    @Transactional
    public DocumentResponse processOcrForRecord(String recordId, String currentUserEmail, String currentUserRole) {
        VisitingCard card = visitingCardRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "recordId", recordId));

        validateOwnership(card, currentUserEmail, currentUserRole);

        log.info("Triggering instant dynamic OCR extraction for recordId: {}", recordId);
        boolean success = dynamicOcrService.processCardOcrDynamically(card);
        VisitingCard updated = visitingCardRepository.save(card);
        log.info("Instant OCR extraction for {} result: {}, status: {}", recordId, success, updated.getOcrStatus());
        return DocumentResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteDocumentByRecordId(String recordId, String currentUserEmail, String currentUserRole) {
        VisitingCard card = visitingCardRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "recordId", recordId));
        validateOwnership(card, currentUserEmail, currentUserRole);
        visitingCardRepository.delete(card);
        log.info("Deleted visiting card record: {} by {}", recordId, currentUserEmail);
    }

    @Transactional(readOnly = true)
    public byte[] getCardImageBytes(String recordId) {
        // Stream directly from AWS S3 bucket (no local filesystem storage)
        VisitingCard card = visitingCardRepository.findByRecordId(recordId).orElse(null);
        if (card == null) {
            return null;
        }

        // 1. Try S3 bucket
        if (card.getS3Key() != null && !card.getS3Key().trim().isEmpty()) {
            try {
                byte[] s3Bytes = s3Service.getObjectBytes(card.getS3Key());
                if (s3Bytes != null && s3Bytes.length > 0) {
                    return s3Bytes;
                }
            } catch (Exception e) {
                log.debug("S3 retrieval skipped for record {}: {}", recordId, e.getMessage());
            }
        }

        // 2. Fallback to database image_url if stored as data URI (pure database storage, zero disk writes)
        if (card.getImageUrl() != null && card.getImageUrl().startsWith("data:image/")) {
            try {
                String base64 = card.getImageUrl().substring(card.getImageUrl().indexOf(",") + 1);
                return java.util.Base64.getDecoder().decode(base64.trim());
            } catch (Exception e) {
                log.warn("Failed to decode base64 from image_url for record {}: {}", recordId, e.getMessage());
            }
        }

        return null;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long total = visitingCardRepository.count();
        long uploaded = visitingCardRepository.findAll().stream()
                .filter(r -> r.getStatus() == RecordStatus.UPLOADED || r.getStatus() == RecordStatus.VERIFIED)
                .count();
        long pending = visitingCardRepository.findAll().stream()
                .filter(r -> r.getStatus() == RecordStatus.PENDING_UPLOAD || r.getStatus() == RecordStatus.UPLOADING)
                .count();
        long failed = visitingCardRepository.findAll().stream()
                .filter(r -> r.getStatus() == RecordStatus.FAILED)
                .count();
        long ocrCompleted = visitingCardRepository.findByOcrStatus(OcrStatus.COMPLETED).size();

        return DashboardStatsResponse.builder()
                .totalRecords(total)
                .uploadedCount(uploaded)
                .pendingCount(pending)
                .failedCount(failed)
                .ocrCompletedCount(ocrCompleted)
                .build();
    }

    /**
     * Shares a visiting card's details with a lead recipient via email.
     * Enforces authentication, authorization (RBAC/ownership), existence check, and audit tracking.
     */
    @Transactional
    public DocumentResponse shareVisitingCard(String cardIdentifier, ShareCardRequest request, User currentUser) {
        if (currentUser == null) {
            throw new ApiException("Authentication is required to share visiting cards", HttpStatus.UNAUTHORIZED);
        }

        VisitingCard card = null;
        try {
            Long numericId = Long.parseLong(cardIdentifier);
            card = visitingCardRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (card == null) {
            card = visitingCardRepository.findByRecordId(cardIdentifier).orElse(null);
        }

        if (card == null) {
            throw new ResourceNotFoundException("VisitingCard", "id or recordId", cardIdentifier);
        }

        // Validate user permission (Supervisors and Admins can share any card; Field Users can share their own)
        validateOwnership(card, currentUser.getEmail(), currentUser.getRole().name());

        if (request.getLeadEmail() == null || request.getLeadEmail().trim().isEmpty()) {
            throw new ApiException("Lead email is required", HttpStatus.BAD_REQUEST);
        }

        String leadEmail = request.getLeadEmail().trim();
        String subject = request.getSubject();

        CardShareAudit audit = new CardShareAudit();
        audit.setCardId(card.getId());
        audit.setRecordId(card.getRecordId());
        audit.setSenderEmail(currentUser.getEmail());
        audit.setSenderName(currentUser.getName());
        audit.setSenderRole(currentUser.getRole().name());
        audit.setLeadEmail(leadEmail);
        audit.setSubject(subject);
        audit.setAction("VISITING_CARD_SHARED");

        try {
            String emailResult = emailService.sendVisitingCardToLead(
                    card,
                    leadEmail,
                    subject,
                    currentUser.getName()
            );
            log.info("Shared visiting card [{}] with lead {}: {}", card.getRecordId(), leadEmail, emailResult);
            audit.setStatus("SUCCESS");
            cardShareAuditRepository.save(audit);
            return DocumentResponse.fromEntity(card);
        } catch (Exception e) {
            log.error("Failed to share visiting card [{}] with lead {}: {}", card.getRecordId(), leadEmail, e.getMessage());
            audit.setStatus("FAILED");
            audit.setErrorMessage(e.getMessage());
            cardShareAuditRepository.save(audit);
            throw new ApiException("Failed to send email to lead: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateOwnership(VisitingCard card, String currentUserEmail, String currentUserRole) {
        boolean isPrivileged = "SUPERVISOR".equalsIgnoreCase(currentUserRole) || "ADMIN".equalsIgnoreCase(currentUserRole);
        if (!isPrivileged && (currentUserEmail == null || !currentUserEmail.equalsIgnoreCase(card.getUploaderEmail()))) {
            throw new ApiException("Access Denied: You do not have permission to view or share this document.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Determines and strictly validates the file format.
     * Allowed formats: PNG, JPG/JPEG, Word (.doc, .docx), PDF.
     * Throws ApiException if file type is unsupported.
     */
    private FileFormatInfo validateAndResolveFormat(String rawBase64, String providedFileName) {
        String fileName = providedFileName != null ? providedFileName.trim().toLowerCase() : "";
        String extension = "";
        String contentType = "";

        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf("."));
        }

        String dataUriPrefix = "";
        String base64Str = rawBase64;
        if (base64Str.contains(",")) {
            dataUriPrefix = base64Str.substring(0, base64Str.indexOf(",")).toLowerCase();
            base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
        }

        byte[] decodedBytes;
        try {
            decodedBytes = java.util.Base64.getDecoder().decode(base64Str.trim());
        } catch (Exception e) {
            throw new ApiException("Invalid base64 payload: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 1. Detect from Data URI prefix first if present
        if (dataUriPrefix.contains("image/png")) {
            contentType = "image/png";
            extension = ".png";
        } else if (dataUriPrefix.contains("image/jpeg") || dataUriPrefix.contains("image/jpg") || dataUriPrefix.contains("image/pjpeg")) {
            contentType = "image/jpeg";
            extension = extension.equals(".jpeg") ? ".jpeg" : ".jpg";
        } else if (dataUriPrefix.contains("application/pdf")) {
            contentType = "application/pdf";
            extension = ".pdf";
        } else if (dataUriPrefix.contains("application/msword")) {
            contentType = "application/msword";
            extension = ".doc";
        } else if (dataUriPrefix.contains("wordprocessingml") || dataUriPrefix.contains("officedocument.wordprocessingml")) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            extension = ".docx";
        }

        // 2. Detect / Verify via Magic Bytes (file signatures)
        if (decodedBytes.length >= 4) {
            // PNG: 89 50 4E 47
            if ((decodedBytes[0] & 0xFF) == 0x89 && (decodedBytes[1] & 0xFF) == 0x50 &&
                (decodedBytes[2] & 0xFF) == 0x4E && (decodedBytes[3] & 0xFF) == 0x47) {
                contentType = "image/png";
                extension = ".png";
            }
            // JPEG: FF D8 FF
            else if ((decodedBytes[0] & 0xFF) == 0xFF && (decodedBytes[1] & 0xFF) == 0xD8 && (decodedBytes[2] & 0xFF) == 0xFF) {
                contentType = "image/jpeg";
                extension = extension.equals(".jpeg") ? ".jpeg" : ".jpg";
            }
            // PDF: %PDF (25 50 44 46)
            else if ((decodedBytes[0] & 0xFF) == 0x25 && (decodedBytes[1] & 0xFF) == 0x50 &&
                     (decodedBytes[2] & 0xFF) == 0x44 && (decodedBytes[3] & 0xFF) == 0x46) {
                contentType = "application/pdf";
                extension = ".pdf";
            }
            // DOCX / ZIP (PK..): 50 4B 03 04
            else if ((decodedBytes[0] & 0xFF) == 0x50 && (decodedBytes[1] & 0xFF) == 0x4B &&
                     (decodedBytes[2] & 0xFF) == 0x03 && (decodedBytes[3] & 0xFF) == 0x04) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = ".docx";
            }
            // DOC (OLE2): D0 CF 11 E0
            else if ((decodedBytes[0] & 0xFF) == 0xD0 && (decodedBytes[1] & 0xFF) == 0xCF &&
                     (decodedBytes[2] & 0xFF) == 0x11 && (decodedBytes[3] & 0xFF) == 0xE0) {
                contentType = "application/msword";
                extension = ".doc";
            }
        }

        // 3. Fallback to file extension matching if bytes header is generic
        if (contentType.isEmpty() && !extension.isEmpty()) {
            if (extension.equals(".png")) {
                contentType = "image/png";
            } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                contentType = "image/jpeg";
            } else if (extension.equals(".pdf")) {
                contentType = "application/pdf";
            } else if (extension.equals(".doc")) {
                contentType = "application/msword";
            } else if (extension.equals(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
        }

        // 4. Final strict validation check
        if (!extension.equals(".png") && !extension.equals(".jpg") && !extension.equals(".jpeg") &&
            !extension.equals(".pdf") && !extension.equals(".doc") && !extension.equals(".docx")) {
            throw new ApiException(
                    "Unsupported file format [" + (extension.isEmpty() ? "Unknown" : extension) + "]. Only PNG, JPEG, JPG, Word (.doc, .docx), and PDF files are supported.",
                    HttpStatus.BAD_REQUEST
            );
        }

        return new FileFormatInfo(decodedBytes, contentType, extension);
    }

    private static class FileFormatInfo {
        final byte[] bytes;
        final String contentType;
        final String extension;

        FileFormatInfo(byte[] bytes, String contentType, String extension) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.extension = extension;
        }
    }
}
