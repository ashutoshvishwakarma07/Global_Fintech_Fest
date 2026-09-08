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

    // 14 Standardized Visiting Card Fields
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

        public DocumentUploadRequestBuilder recordId(String recordId) { this.recordId = recordId; return this; }
        public DocumentUploadRequestBuilder uploaderName(String uploaderName) { this.uploaderName = uploaderName; return this; }
        public DocumentUploadRequestBuilder uploaderEmail(String uploaderEmail) { this.uploaderEmail = uploaderEmail; return this; }
        public DocumentUploadRequestBuilder uploaderMobile(String uploaderMobile) { this.uploaderMobile = uploaderMobile; return this; }
        public DocumentUploadRequestBuilder uploaderRole(UserRole uploaderRole) { this.uploaderRole = uploaderRole; return this; }
        public DocumentUploadRequestBuilder notes(String notes) { this.notes = notes; return this; }
        public DocumentUploadRequestBuilder imageBase64(String imageBase64) { this.imageBase64 = imageBase64; return this; }
        public DocumentUploadRequestBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public DocumentUploadRequestBuilder fileSize(String fileSize) { this.fileSize = fileSize; return this; }
        public DocumentUploadRequestBuilder isOffline(Boolean isOffline) { this.isOffline = isOffline; return this; }
        public DocumentUploadRequestBuilder name(String name) { this.name = name; this.cardHolderName = name; return this; }
        public DocumentUploadRequestBuilder cardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; this.name = cardHolderName; return this; }
        public DocumentUploadRequestBuilder jobTitle(String jobTitle) { this.jobTitle = jobTitle; this.designation = jobTitle; return this; }
        public DocumentUploadRequestBuilder designation(String designation) { this.designation = designation; this.jobTitle = designation; return this; }
        public DocumentUploadRequestBuilder companyName(String companyName) { this.companyName = companyName; return this; }
        public DocumentUploadRequestBuilder department(String department) { this.department = department; return this; }
        public DocumentUploadRequestBuilder emailAddress(String emailAddress) { this.emailAddress = emailAddress; this.extractedEmail = emailAddress; return this; }
        public DocumentUploadRequestBuilder extractedEmail(String extractedEmail) { this.extractedEmail = extractedEmail; this.emailAddress = extractedEmail; return this; }
        public DocumentUploadRequestBuilder mobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; this.extractedMobile = mobileNumber; return this; }
        public DocumentUploadRequestBuilder extractedMobile(String extractedMobile) { this.extractedMobile = extractedMobile; this.mobileNumber = extractedMobile; return this; }
        public DocumentUploadRequestBuilder workNumber(String workNumber) { this.workNumber = workNumber; return this; }
        public DocumentUploadRequestBuilder websiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; return this; }
        public DocumentUploadRequestBuilder city(String city) { this.city = city; return this; }
        public DocumentUploadRequestBuilder state(String state) { this.state = state; return this; }
        public DocumentUploadRequestBuilder postalZipCode(String postalZipCode) { this.postalZipCode = postalZipCode; return this; }
        public DocumentUploadRequestBuilder country(String country) { this.country = country; return this; }
        public DocumentUploadRequestBuilder linkedIn(String linkedIn) { this.linkedIn = linkedIn; return this; }
        public DocumentUploadRequestBuilder twitter(String twitter) { this.twitter = twitter; return this; }
        public DocumentUploadRequestBuilder extractedAddress(String extractedAddress) { this.extractedAddress = extractedAddress; return this; }
        public DocumentUploadRequestBuilder rawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; return this; }

        public DocumentUploadRequest build() {
            DocumentUploadRequest req = new DocumentUploadRequest(recordId, uploaderName, uploaderEmail, uploaderMobile, uploaderRole, notes, imageBase64, fileName, fileSize, isOffline);
            req.setName(this.name != null ? this.name : this.cardHolderName);
            req.setCardHolderName(this.cardHolderName != null ? this.cardHolderName : this.name);
            req.setJobTitle(this.jobTitle != null ? this.jobTitle : this.designation);
            req.setDesignation(this.designation != null ? this.designation : this.jobTitle);
            req.setCompanyName(this.companyName);
            req.setDepartment(this.department);
            req.setEmailAddress(this.emailAddress != null ? this.emailAddress : this.extractedEmail);
            req.setExtractedEmail(this.extractedEmail != null ? this.extractedEmail : this.emailAddress);
            req.setMobileNumber(this.mobileNumber != null ? this.mobileNumber : this.extractedMobile);
            req.setExtractedMobile(this.extractedMobile != null ? this.extractedMobile : this.mobileNumber);
            req.setWorkNumber(this.workNumber);
            req.setWebsiteUrl(this.websiteUrl);
            req.setCity(this.city);
            req.setState(this.state);
            req.setPostalZipCode(this.postalZipCode);
            req.setCountry(this.country);
            req.setLinkedIn(this.linkedIn);
            req.setTwitter(this.twitter);
            req.setExtractedAddress(this.extractedAddress);
            req.setRawOcrText(this.rawOcrText);
            return req;
        }
    }

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }

    public Boolean getIsOffline() { return isOffline; }
    public void setIsOffline(Boolean isOffline) { this.isOffline = isOffline; }

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
}
