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

    // 14 Standardized Visiting Card Fields
    private OcrStatus ocrStatus;
    private String name;
    private String cardHolderName;
    private String jobTitle;
    private String designation;
    private String companyName;
    private String department;
    private String emailAddress;
    private String extractedEmail;
    private String mobileNumber;
    private String extractedMobile;
    private String workNumber;
    private String websiteUrl;
    private String city;
    private String state;
    private String postalZipCode;
    private String country;
    private String linkedIn;
    private String twitter;
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
        String img = card.getImageUrl();
        if (img == null || img.trim().isEmpty()) {
            img = "/api/v1/documents/record/" + card.getRecordId() + "/image";
        }
        response.setImageUrl(img);
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

        // 14 Standardized Fields
        response.setName(card.getCardHolderName());
        response.setCardHolderName(card.getCardHolderName());
        response.setJobTitle(card.getDesignation());
        response.setDesignation(card.getDesignation());
        response.setCompanyName(card.getCompanyName());
        response.setDepartment(card.getDepartment());
        response.setEmailAddress(card.getExtractedEmail());
        response.setExtractedEmail(card.getExtractedEmail());
        response.setMobileNumber(card.getExtractedMobile());
        response.setExtractedMobile(card.getExtractedMobile());
        response.setWorkNumber(card.getWorkNumber());
        response.setWebsiteUrl(card.getWebsiteUrl());
        response.setCity(card.getCity());
        response.setState(card.getState());
        response.setPostalZipCode(card.getPostalZipCode());
        response.setCountry(card.getCountry());
        response.setLinkedIn(card.getLinkedIn());
        response.setTwitter(card.getTwitter());

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

    // 1. Name
    public String getName() { return name != null ? name : cardHolderName; }
    public void setName(String name) { this.name = name; if (this.cardHolderName == null) this.cardHolderName = name; }
    public String getCardHolderName() { return cardHolderName != null ? cardHolderName : name; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; if (this.name == null) this.name = cardHolderName; }

    // 2. Job Title
    public String getJobTitle() { return jobTitle != null ? jobTitle : designation; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; if (this.designation == null) this.designation = jobTitle; }
    public String getDesignation() { return designation != null ? designation : jobTitle; }
    public void setDesignation(String designation) { this.designation = designation; if (this.jobTitle == null) this.jobTitle = designation; }

    // 3. Company Name
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    // 4. Department
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    // 5. Email Address
    public String getEmailAddress() { return emailAddress != null ? emailAddress : extractedEmail; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; if (this.extractedEmail == null) this.extractedEmail = emailAddress; }
    public String getExtractedEmail() { return extractedEmail != null ? extractedEmail : emailAddress; }
    public void setExtractedEmail(String extractedEmail) { this.extractedEmail = extractedEmail; if (this.emailAddress == null) this.emailAddress = extractedEmail; }

    // 6. Mobile Number
    public String getMobileNumber() { return mobileNumber != null ? mobileNumber : extractedMobile; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; if (this.extractedMobile == null) this.extractedMobile = mobileNumber; }
    public String getExtractedMobile() { return extractedMobile != null ? extractedMobile : mobileNumber; }
    public void setExtractedMobile(String extractedMobile) { this.extractedMobile = extractedMobile; if (this.mobileNumber == null) this.mobileNumber = extractedMobile; }

    // 7. Work Number
    public String getWorkNumber() { return workNumber; }
    public void setWorkNumber(String workNumber) { this.workNumber = workNumber; }

    // 8. Website URL
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    // 9. City
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    // 10. State
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    // 11. Postal / ZIP Code
    public String getPostalZipCode() { return postalZipCode; }
    public void setPostalZipCode(String postalZipCode) { this.postalZipCode = postalZipCode; }

    // 12. Country
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    // 13. LinkedIn
    public String getLinkedIn() { return linkedIn; }
    public void setLinkedIn(String linkedIn) { this.linkedIn = linkedIn; }

    // 14. Twitter / X
    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }

    // Address & Raw OCR
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
