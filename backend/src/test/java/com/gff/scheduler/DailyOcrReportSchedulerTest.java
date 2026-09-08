package com.gff.scheduler;

import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.repository.VisitingCardRepository;
import com.gff.service.DynamicOcrService;
import com.gff.service.EmailService;
import com.gff.service.ExcelReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyOcrReportSchedulerTest {

    @Mock
    private VisitingCardRepository visitingCardRepository;

    @Mock
    private DynamicOcrService dynamicOcrService;

    @Mock
    private ExcelReportService excelReportService;

    @Mock
    private EmailService emailService;

    private DailyOcrReportScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DailyOcrReportScheduler(
                visitingCardRepository,
                dynamicOcrService,
                excelReportService,
                emailService
        );
    }

    @Test
    void runDailyReportWorkflow_NoPendingCards_DoesNotSendEmail() {
        when(visitingCardRepository.findByOcrStatusIn(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = scheduler.runDailyReportWorkflow();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(0, result.get("pendingRecordsFound"));
        assertEquals(0, result.get("recordsReadyForExport"));
        assertEquals("NO", result.get("emailSent"));
        assertEquals(0, result.get("recordsMarkedAsEmailed"));

        verifyNoInteractions(excelReportService);
        verifyNoInteractions(emailService);
    }

    @Test
    void runDailyReportWorkflow_NewlyCompletedCards_GeneratesReportAndSendsEmail() {
        VisitingCard card = VisitingCard.builder()
                .id(1L)
                .recordId("REC-001")
                .ocrStatus(OcrStatus.PENDING)
                .uploaderEmail("user1@demo.com")
                .build();

        when(visitingCardRepository.findByOcrStatusIn(any())).thenReturn(List.of(card));
        when(dynamicOcrService.processCardOcrDynamically(card)).thenAnswer(inv -> {
            card.setOcrStatus(OcrStatus.COMPLETED);
            return true;
        });

        byte[] fakeExcel = new byte[]{1, 2, 3};
        when(excelReportService.generateDailyOcrReport(any(), any(), any())).thenReturn(fakeExcel);
        when(emailService.sendDailyOcrReport(eq(fakeExcel), any(), any(), any())).thenReturn("SUCCESS: Email delivered");

        Map<String, Object> result = scheduler.runDailyReportWorkflow();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(1, result.get("pendingRecordsFound"));
        assertEquals(1, result.get("ocrCompleted"));
        assertEquals(0, result.get("ocrFailed"));
        assertEquals(1, result.get("recordsReadyForExport"));
        assertEquals("YES", result.get("emailSent"));
        assertEquals(1, result.get("recordsMarkedAsEmailed"));

        assertNotNull(card.getEmailSentAt(), "card.emailSentAt should be populated with timestamp");
        verify(visitingCardRepository, atLeastOnce()).save(card);
    }

    @Test
    void runDailyReportWorkflow_EmailFailure_DoesNotMarkCardsAsEmailed() {
        VisitingCard card = VisitingCard.builder()
                .id(2L)
                .recordId("REC-002")
                .ocrStatus(OcrStatus.PENDING)
                .build();

        when(visitingCardRepository.findByOcrStatusIn(any())).thenReturn(List.of(card));
        when(dynamicOcrService.processCardOcrDynamically(card)).thenAnswer(inv -> {
            card.setOcrStatus(OcrStatus.COMPLETED);
            return true;
        });

        byte[] fakeExcel = new byte[]{1, 2, 3};
        when(excelReportService.generateDailyOcrReport(any(), any(), any())).thenReturn(fakeExcel);
        when(emailService.sendDailyOcrReport(eq(fakeExcel), any(), any(), any())).thenReturn("ERROR: Connection timed out");

        Map<String, Object> result = scheduler.runDailyReportWorkflow();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals("NO", result.get("emailSent"));
        assertEquals(0, result.get("recordsMarkedAsEmailed"));

        assertNull(card.getEmailSentAt(), "card.emailSentAt must remain null on failure so it can be retried");
    }

    @Test
    void runDailyReportWorkflow_AlreadyEmailedCards_AreNotEmailedAgain() {
        VisitingCard card = VisitingCard.builder()
                .id(3L)
                .recordId("REC-003")
                .ocrStatus(OcrStatus.PENDING)
                .emailSentAt(LocalDateTime.now().minusDays(1)) // already emailed previously
                .build();

        when(visitingCardRepository.findByOcrStatusIn(any())).thenReturn(List.of(card));
        when(dynamicOcrService.processCardOcrDynamically(card)).thenAnswer(inv -> {
            card.setOcrStatus(OcrStatus.COMPLETED);
            return true;
        });

        Map<String, Object> result = scheduler.runDailyReportWorkflow();

        assertEquals(0, result.get("recordsReadyForExport"));
        assertEquals("NO", result.get("emailSent"));

        verifyNoInteractions(excelReportService);
        verifyNoInteractions(emailService);
    }
}
