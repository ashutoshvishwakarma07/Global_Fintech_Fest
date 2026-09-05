package com.gff.entity;

import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.entity.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing an uploaded Visiting Card record,
 * its storage metadata, lifecycle status, and OCR extracted fields.
 */
@Entity
@Table(name = "visiting_cards", indexes = {
        @Index(name = "idx_record_id", columnList = "record_id", unique = true),
        @Index(name = "idx_uploader_email", columnList = "uploader_email"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_ocr_status", columnList = "ocr_status")
})
public class VisitingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false, unique = true, length = 64)
    private String recordId;

    // --- Uploader Details ---
    @Column(name = "uploader_name", nullable = false)
    private String uploaderName;

    @Column(name = "uploader_email", nullable = false)
    private String uploaderEmail;

    @Column(name = "uploader_mobile", length = 32)
    private String uploaderMobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploader_role", nullable = false, length = 32)
    private UserRole uploaderRole;

    // --- Media & S3 Storage Details ---
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "s3_key", length = 512)
    private String s3Key;

    @Column(name = "s3_bucket", length = 128)
    private String s3Bucket = "visiting-card-bkt";

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size", length = 32)
    private String fileSize;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // --- Record & Sync Status ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RecordStatus status = RecordStatus.PENDING_UPLOAD;

    @Column(name = "is_offline")
    private Boolean isOffline = false;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // --- OCR Fields ---
    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status", nullable = false, length = 32)
    private OcrStatus ocrStatus = OcrStatus.PENDING;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "designation")
    private String designation;

    @Column(name = "extracted_email")
    private String extractedEmail;

    @Column(name = "extracted_mobile", length = 32)
    private String extractedMobile;

    @Column(name = "extracted_address", columnDefinition = "TEXT")
    private String extractedAddress;

    @Column(name = "raw_ocr_text", columnDefinition = "TEXT")
    private String rawOcrText;

    @Column(name = "ocr_processed_at")
    private LocalDateTime ocrProcessedAt;

    // --- Timestamps ---
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VisitingCard() {
    }

    public static VisitingCardBuilder builder() {
        return new VisitingCardBuilder();
    }

    public static class VisitingCardBuilder {
        private Long id;
        private String recordId;
        private String uploaderName;
        private String uploaderEmail;
        private String uploaderMobile;
        private UserRole uploaderRole;
        private String imageUrl;
        private String s3Key;
        private String s3Bucket = "visiting-card-bkt";
        private String fileName;
        private String fileSize;
        private String notes;
        private RecordStatus status = RecordStatus.PENDING_UPLOAD;
        private Boolean isOffline = false;
        private Integer retryCount = 0;
        private String errorMessage;
        private OcrStatus ocrStatus = OcrStatus.PENDING;
        private String cardHolderName;
        private String companyName;
        private String designation;
        private String extractedEmail;
        private String extractedMobile;
        private String extractedAddress;
        private String rawOcrText;
        private LocalDateTime ocrProcessedAt;

        public VisitingCardBuilder id(Long id) { this.id = id; return this; }
        public VisitingCardBuilder recordId(String recordId) { this.recordId = recordId; return this; }
        public VisitingCardBuilder uploaderName(String uploaderName) { this.uploaderName = uploaderName; return this; }
        public VisitingCardBuilder uploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; return this; }
        public VisitingCardBuilder uploaderMobile(String uploaderMobile) { this.uploaderMobile = uploaderMobile; return this; }
        public VisitingCardBuilder uploaderRole(UserRole uploaderRole) { this.uploaderRole = uploaderRole; return this; }
        public VisitingCardBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public VisitingCardBuilder s3Key(String s3Key) { this.s3Key = s3Key; return this; }
        public VisitingCardBuilder s3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; return this; }
        public VisitingCardBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public VisitingCardBuilder fileSize(String fileSize) { this.fileSize = fileSize; return this; }
        public VisitingCardBuilder notes(String notes) { this.notes = notes; return this; }
        public VisitingCardBuilder status(RecordStatus status) { this.status = status; return this; }
        public VisitingCardBuilder isOffline(Boolean isOffline) { this.isOffline = isOffline; return this; }
        public VisitingCardBuilder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public VisitingCardBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public VisitingCardBuilder ocrStatus(OcrStatus ocrStatus) { this.ocrStatus = ocrStatus; return this; }
        public VisitingCardBuilder cardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; return this; }
        public VisitingCardBuilder companyName(String companyName) { this.companyName = companyName; return this; }
        public VisitingCardBuilder designation(String designation) { this.designation = designation; return this; }
        public VisitingCardBuilder extractedEmail(String extractedEmail) { this.extractedEmail = extractedEmail; return this; }
        public VisitingCardBuilder extractedMobile(String extractedMobile) { this.extractedMobile = extractedMobile; return this; }
        public VisitingCardBuilder extractedAddress(String extractedAddress) { this.extractedAddress = extractedAddress; return this; }
        public VisitingCardBuilder rawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; return this; }
        public VisitingCardBuilder ocrProcessedAt(LocalDateTime ocrProcessedAt) { this.ocrProcessedAt = ocrProcessedAt; return this; }

        public VisitingCard build() {
            VisitingCard card = new VisitingCard();
            card.id = this.id;
            card.recordId = this.recordId;
            card.uploaderName = this.uploaderName;
            card.uploaderEmail = this.uploaderEmail;
            card.uploaderMobile = this.uploaderMobile;
            card.uploaderRole = this.uploaderRole;
            card.imageUrl = this.imageUrl;
            card.s3Key = this.s3Key;
            card.s3Bucket = this.s3Bucket;
            card.fileName = this.fileName;
            card.fileSize = this.fileSize;
            card.notes = this.notes;
            card.status = this.status;
            card.isOffline = this.isOffline;
            card.retryCount = this.retryCount;
            card.errorMessage = this.errorMessage;
            card.ocrStatus = this.ocrStatus;
            card.cardHolderName = this.cardHolderName;
            card.companyName = this.companyName;
            card.designation = this.designation;
            card.extractedEmail = this.extractedEmail;
            card.extractedMobile = this.extractedMobile;
            card.extractedAddress = this.extractedAddress;
            card.rawOcrText = this.rawOcrText;
            card.ocrProcessedAt = this.ocrProcessedAt;
            return card;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = RecordStatus.PENDING_UPLOAD;
        if (this.ocrStatus == null) this.ocrStatus = OcrStatus.PENDING;
        if (this.isOffline == null) this.isOffline = false;
        if (this.retryCount == null) this.retryCount = 0;
        if (this.s3Bucket == null) this.s3Bucket = "visiting-card-bkt";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public String getUploaderEmail() { return uploaderEmail; }
    public void setUploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; }

    public String getUploaderMobile() { return uploaderMobile; }
    public void setUploaderMobile(String uploaderMobile) { this.uploaderMobile = uploaderMobile; }

    public UserRole getUploaderRole() { return uploaderRole; }
    public void setUploaderRole(UserRole uploaderRole) { this.uploaderRole = uploaderRole; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public RecordStatus getStatus() { return status; }
    public void setStatus(RecordStatus status) { this.status = status; }

    public Boolean getIsOffline() { return isOffline; }
    public void setIsOffline(Boolean isOffline) { this.isOffline = isOffline; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OcrStatus getOcrStatus() { return ocrStatus; }
    public void setOcrStatus(OcrStatus ocrStatus) { this.ocrStatus = ocrStatus; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getExtractedEmail() { return extractedEmail; }
    public void setExtractedEmail(String extractedEmail) { this.extractedEmail = extractedEmail; }

    public String getExtractedMobile() { return extractedMobile; }
    public void setExtractedMobile(String extractedMobile) { this.extractedMobile = extractedMobile; }

    public String getExtractedAddress() { return extractedAddress; }
    public void setExtractedAddress(String extractedAddress) { this.extractedAddress = extractedAddress; }

    public String getRawOcrText() { return rawOcrText; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }

    public LocalDateTime getOcrProcessedAt() { return ocrProcessedAt; }
    public void setOcrProcessedAt(LocalDateTime ocrProcessedAt) { this.ocrProcessedAt = ocrProcessedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
