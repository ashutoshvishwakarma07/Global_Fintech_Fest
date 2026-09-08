package com.gff.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an audit log for visiting card sharing events.
 */
@Entity
@Table(name = "card_share_audits", indexes = {
        @Index(name = "idx_csa_card_id", columnList = "card_id"),
        @Index(name = "idx_csa_sender_email", columnList = "sender_email"),
        @Index(name = "idx_csa_lead_email", columnList = "lead_email"),
        @Index(name = "idx_csa_created_at", columnList = "created_at")
})
public class CardShareAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "record_id", length = 64)
    private String recordId;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_role", length = 32)
    private String senderRole;

    @Column(name = "lead_email", nullable = false)
    private String leadEmail;

    @Column(name = "subject", length = 512)
    private String subject;

    @Column(name = "action", nullable = false, length = 64)
    private String action = "VISITING_CARD_SHARED";

    @Column(name = "status", nullable = false, length = 32)
    private String status; // SUCCESS / FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CardShareAudit() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.action == null) {
            this.action = "VISITING_CARD_SHARED";
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getLeadEmail() { return leadEmail; }
    public void setLeadEmail(String leadEmail) { this.leadEmail = leadEmail; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
