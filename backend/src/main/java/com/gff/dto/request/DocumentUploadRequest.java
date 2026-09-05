package com.gff.dto.request;

import com.gff.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class DocumentUploadRequest {

    private String recordId;

    @NotBlank(message = "Uploader name is required")
    private String uploaderName;

    @NotBlank(message = "Uploader email is required")
    @Email(message = "Invalid uploader email format")
    private String uploaderEmail;

    private String uploaderMobile;
    private UserRole uploaderRole;
    private String notes;
    private String imageBase64;
    private String fileName;
    private String fileSize;
    private Boolean isOffline;

    // Optional dynamic OCR fields if client-side OCR was performed
    private String cardHolderName;
    private String companyName;
    private String designation;
    private String extractedEmail;
    private String extractedMobile;
    private String extractedAddress;
    private String rawOcrText;

    public DocumentUploadRequest() {
    }

    public DocumentUploadRequest(String recordId, String uploaderName, String uploaderEmail, String uploaderMobile, UserRole uploaderRole, String notes, String imageBase64, String fileName, String fileSize, Boolean isOffline) {
        this.recordId = recordId;
        this.uploaderName = uploaderName;
        this.uploaderEmail = uploaderEmail;
        this.uploaderMobile = uploaderMobile;
        this.uploaderRole = uploaderRole;
        this.notes = notes;
        this.imageBase64 = imageBase64;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.isOffline = isOffline;
    }

    public static DocumentUploadRequestBuilder builder() {
        return new DocumentUploadRequestBuilder();
    }

    public static class DocumentUploadRequestBuilder {
        private String recordId;
        private String uploaderName;
        private String uploaderEmail;
        private String uploaderMobile;
        private UserRole uploaderRole;
        private String notes;
        private String imageBase64;
        private String fileName;
        private String fileSize;
        private Boolean isOffline;

        public DocumentUploadRequestBuilder recordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        public DocumentUploadRequestBuilder uploaderName(String uploaderName) {
            this.uploaderName = uploaderName;
            return this;
        }

        public DocumentUploadRequestBuilder uploaderEmail(String uploaderEmail) {
            this.uploaderEmail = uploaderEmail;
            return this;
        }

        public DocumentUploadRequestBuilder uploaderMobile(String uploaderMobile) {
            this.uploaderMobile = uploaderMobile;
            return this;
        }

        public DocumentUploadRequestBuilder uploaderRole(UserRole uploaderRole) {
            this.uploaderRole = uploaderRole;
            return this;
        }

        public DocumentUploadRequestBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public DocumentUploadRequestBuilder imageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
            return this;
        }

        public DocumentUploadRequestBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public DocumentUploadRequestBuilder fileSize(String fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public DocumentUploadRequestBuilder isOffline(Boolean isOffline) {
            this.isOffline = isOffline;
            return this;
        }

        public DocumentUploadRequest build() {
            return new DocumentUploadRequest(recordId, uploaderName, uploaderEmail, uploaderMobile, uploaderRole, notes, imageBase64, fileName, fileSize, isOffline);
        }
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getUploaderEmail() {
        return uploaderEmail;
    }

    public void setUploaderEmail(String uploaderEmail) {
        this.uploaderEmail = uploaderEmail;
    }

    public String getUploaderMobile() {
        return uploaderMobile;
    }

    public void setUploaderMobile(String uploaderMobile) {
        this.uploaderMobile = uploaderMobile;
    }

    public UserRole getUploaderRole() {
        return uploaderRole;
    }

    public void setUploaderRole(UserRole uploaderRole) {
        this.uploaderRole = uploaderRole;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public Boolean getIsOffline() {
        return isOffline;
    }

    public void setIsOffline(Boolean isOffline) {
        this.isOffline = isOffline;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getExtractedEmail() {
        return extractedEmail;
    }

    public void setExtractedEmail(String extractedEmail) {
        this.extractedEmail = extractedEmail;
    }

    public String getExtractedMobile() {
        return extractedMobile;
    }

    public void setExtractedMobile(String extractedMobile) {
        this.extractedMobile = extractedMobile;
    }

    public String getExtractedAddress() {
        return extractedAddress;
    }

    public void setExtractedAddress(String extractedAddress) {
        this.extractedAddress = extractedAddress;
    }

    public String getRawOcrText() {
        return rawOcrText;
    }

    public void setRawOcrText(String rawOcrText) {
        this.rawOcrText = rawOcrText;
    }
}
