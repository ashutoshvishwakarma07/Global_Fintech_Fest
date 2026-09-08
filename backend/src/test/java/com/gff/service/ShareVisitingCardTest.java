package com.gff.service;

import com.gff.dto.request.ShareCardRequest;
import com.gff.dto.response.DocumentResponse;
import com.gff.entity.CardShareAudit;
import com.gff.entity.User;
import com.gff.entity.VisitingCard;
import com.gff.entity.enums.UserRole;
import com.gff.exception.ApiException;
import com.gff.exception.ResourceNotFoundException;
import com.gff.repository.CardShareAuditRepository;
import com.gff.repository.VisitingCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareVisitingCardTest {

    @Mock
    private VisitingCardRepository visitingCardRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private DynamicOcrService dynamicOcrService;

    @Mock
    private EmailService emailService;

    @Mock
    private CardShareAuditRepository cardShareAuditRepository;

    private DocumentService documentService;

    private User adminUser;
    private User fieldUser;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                visitingCardRepository,
                s3Service,
                dynamicOcrService,
                emailService,
                cardShareAuditRepository
        );

        adminUser = User.builder()
                .id(1L)
                .email("admin@demo.com")
                .name("Admin User")
                .role(UserRole.ADMIN)
                .build();

        fieldUser = User.builder()
                .id(2L)
                .email("user1@demo.com")
                .name("Field User 1")
                .role(UserRole.FIELD_USER)
                .build();
    }

    @Test
    void shareVisitingCard_Success() {
        VisitingCard card = VisitingCard.builder()
                .id(10L)
                .recordId("REC-10")
                .uploaderEmail("user1@demo.com")
                .cardHolderName("Rahul Sharma")
                .companyName("ABC Finance")
                .build();

        when(visitingCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(emailService.sendVisitingCardToLead(eq(card), eq("lead@client.com"), any(), eq("Admin User")))
                .thenReturn("SUCCESS: Sent");

        ShareCardRequest request = new ShareCardRequest("lead@client.com", "Visiting Card Details");
        DocumentResponse response = documentService.shareVisitingCard("10", request, adminUser);

        assertNotNull(response);
        assertEquals("REC-10", response.getRecordId());

        ArgumentCaptor<CardShareAudit> auditCaptor = ArgumentCaptor.forClass(CardShareAudit.class);
        verify(cardShareAuditRepository).save(auditCaptor.capture());

        CardShareAudit savedAudit = auditCaptor.getValue();
        assertEquals("SUCCESS", savedAudit.getStatus());
        assertEquals("lead@client.com", savedAudit.getLeadEmail());
        assertEquals("admin@demo.com", savedAudit.getSenderEmail());
        assertEquals(10L, savedAudit.getCardId());
    }

    @Test
    void shareVisitingCard_CardNotFound_ThrowsResourceNotFound() {
        when(visitingCardRepository.findById(999L)).thenReturn(Optional.empty());
        when(visitingCardRepository.findByRecordId("999")).thenReturn(Optional.empty());

        ShareCardRequest request = new ShareCardRequest("lead@client.com", "Test");
        assertThrows(ResourceNotFoundException.class, () ->
                documentService.shareVisitingCard("999", request, adminUser)
        );
    }

    @Test
    void shareVisitingCard_UnauthorizedFieldUser_ThrowsForbidden() {
        VisitingCard card = VisitingCard.builder()
                .id(20L)
                .recordId("REC-20")
                .uploaderEmail("otheruser@demo.com") // belongs to another user
                .build();

        when(visitingCardRepository.findById(20L)).thenReturn(Optional.of(card));

        ShareCardRequest request = new ShareCardRequest("lead@client.com", "Test");

        // Field user 1 cannot share other user's card
        ApiException ex = assertThrows(ApiException.class, () ->
                documentService.shareVisitingCard("20", request, fieldUser)
        );
        assertTrue(ex.getMessage().contains("Access Denied"));
        verifyNoInteractions(emailService);
    }

    @Test
    void shareVisitingCard_EmailFailure_LogsFailedAudit() {
        VisitingCard card = VisitingCard.builder()
                .id(30L)
                .recordId("REC-30")
                .uploaderEmail("admin@demo.com")
                .build();

        when(visitingCardRepository.findById(30L)).thenReturn(Optional.of(card));
        when(emailService.sendVisitingCardToLead(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("SMTP host connection refused"));

        ShareCardRequest request = new ShareCardRequest("lead@client.com", "Subject");

        assertThrows(ApiException.class, () ->
                documentService.shareVisitingCard("30", request, adminUser)
        );

        ArgumentCaptor<CardShareAudit> auditCaptor = ArgumentCaptor.forClass(CardShareAudit.class);
        verify(cardShareAuditRepository).save(auditCaptor.capture());

        CardShareAudit savedAudit = auditCaptor.getValue();
        assertEquals("FAILED", savedAudit.getStatus());
        assertTrue(savedAudit.getErrorMessage().contains("SMTP host connection refused"));
    }
}
