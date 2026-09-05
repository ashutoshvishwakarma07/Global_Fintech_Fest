package com.gff.dto.response;

import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.entity.enums.UserRole;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String recordId;
    private String uploaderName;
    private String uploaderEmail;
    private String uploaderMobile;
    private UserRole uploaderRole;
    private String imageUrl;
    private String s3Key;
    private String s3Bucket;
    private String fileName;
    private String fileSize;
    private String notes;
    private RecordStatus status;
    private Boolean isOffline;
    private Integer retryCount;
    private String errorMessage;

    // OCR Results
    private OcrStatus ocrStatus;
    private String cardHolderName;
    private String companyName;
    private String designation;
    private String extractedEmail;
    private String extractedMobile;
    private String extractedAddress;
    private String rawOcrText;
    private LocalDateTime ocrProcessedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {
    }

    public static DocumentResponse fromEntity(VisitingCard card) {
        if (card == null) return null;
        DocumentResponse response = new DocumentResponse();
        response.setId(card.getId());
        response.setRecordId(card.getRecordId());
        response.setUploaderName(card.getUploaderName());
        response.setUploaderEmail(card.getUploaderEmail());
        response.setUploaderMobile(card.getUploaderMobile());
        response.setUploaderRole(card.getUploaderRole());
        response.setImageUrl(card.getImageUrl());
        response.setS3Key(card.getS3Key());
        response.setS3Bucket(card.getS3Bucket());
        response.setFileName(card.getFileName());
        response.setFileSize(card.getFileSize());
        response.setNotes(card.getNotes());
        response.setStatus(card.getStatus());
        response.setIsOffline(card.getIsOffline());
        response.setRetryCount(card.getRetryCount());
        response.setErrorMessage(card.getErrorMessage());
        response.setOcrStatus(card.getOcrStatus());
        response.setCardHolderName(card.getCardHolderName());
        response.setCompanyName(card.getCompanyName());
        response.setDesignation(card.getDesignation());
        response.setExtractedEmail(card.getExtractedEmail());
        response.setExtractedMobile(card.getExtractedMobile());
        response.setExtractedAddress(card.getExtractedAddress());
        response.setRawOcrText(card.getRawOcrText());
        response.setOcrProcessedAt(card.getOcrProcessedAt());
        response.setCreatedAt(card.getCreatedAt());
        response.setUpdatedAt(card.getUpdatedAt());
        return response;
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
