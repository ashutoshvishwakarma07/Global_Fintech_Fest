package com.gff.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ShareCardRequest {

    @NotBlank(message = "Lead email is required")
    @Email(message = "Please provide a valid lead email address")
    private String leadEmail;

    private String subject;

    public ShareCardRequest() {
    }

    public ShareCardRequest(String leadEmail, String subject) {
        this.leadEmail = leadEmail;
        this.subject = subject;
    }

    public String getLeadEmail() {
        return leadEmail;
    }

    public void setLeadEmail(String leadEmail) {
        this.leadEmail = leadEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
