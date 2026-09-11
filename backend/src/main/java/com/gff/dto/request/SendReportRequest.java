package com.gff.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for triggering report generation and email dispatch
 * with dynamic To and CC recipients.
 */
public class SendReportRequest {

    @NotEmpty(message = "At least one recipient (To) email address is required")
    private List<String> to = new ArrayList<>();

    private List<String> cc = new ArrayList<>();

    public SendReportRequest() {
    }

    public SendReportRequest(List<String> to, List<String> cc) {
        this.to = to != null ? to : new ArrayList<>();
        this.cc = cc != null ? cc : new ArrayList<>();
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to != null ? to : new ArrayList<>();
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc != null ? cc : new ArrayList<>();
    }
}
