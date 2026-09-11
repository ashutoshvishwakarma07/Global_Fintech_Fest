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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void runTodayReportWorkflow_NoRecords_DoesNotFail() {
        when(visitingCardRepository.findByCreatedAtBetween(any(), any())).thenReturn(Collections.emptyList());
        when(visitingCardRepository.findByOcrStatusAndEmailSentAtIsNull(any())).thenReturn(Collections.emptyList());
        when(excelReportService.generateReport(any(), any(), any(), any(), any())).thenReturn(new byte[]{1, 2});
        when(emailService.sendReport(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("SUCCESS: Email delivered");

        Map<String, Object> result = scheduler.runTodayReportWorkflow();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals("Today's Report", result.get("reportType"));
        assertEquals(0L, result.get("totalRecords"));
        assertEquals("YES", result.get("emailSent"));
    }

    @Test
    void runTodayReportWorkflow_WithCompletedCards_CalculatesUserWiseCountsAndSendsEmail() {
        VisitingCard card1 = VisitingCard.builder()
                .id(1L)
                .recordId("REC-001")
                .ocrStatus(OcrStatus.COMPLETED)
                .uploaderName("Rahul Sharma")
                .uploaderEmail("user1@demo.com")
                .build();
        VisitingCard card2 = VisitingCard.builder()
                .id(2L)
                .recordId("REC-002")
                .ocrStatus(OcrStatus.COMPLETED)
                .uploaderName("Rahul Sharma")
                .uploaderEmail("user1@demo.com")
                .build();
        VisitingCard card3 = VisitingCard.builder()
                .id(3L)
                .recordId("REC-003")
                .ocrStatus(OcrStatus.COMPLETED)
                .uploaderName("Priya Verma")
                .uploaderEmail("user2@demo.com")
                .build();

        when(visitingCardRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(card1, card2, card3));

        byte[] fakeExcel = new byte[]{1, 2, 3};
        when(excelReportService.generateReport(any(), eq("Today's OCR & Upload Summary"), any(), any(), any())).thenReturn(fakeExcel);
        when(emailService.sendReport(eq("Today's Report"), eq(fakeExcel), any(), any(), any(), any(), any(), any(), any())).thenReturn("SUCCESS: Email delivered");

        Map<String, Object> result = scheduler.runTodayReportWorkflow();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(3L, result.get("totalRecords"));
        assertEquals(3, result.get("completedRecords"));
        assertEquals("YES", result.get("emailSent"));

        @SuppressWarnings("unchecked")
        Map<String, Long> userWiseCounts = (Map<String, Long>) result.get("userWiseCounts");
        assertNotNull(userWiseCounts);
        assertEquals(2L, userWiseCounts.get("Rahul Sharma"));
        assertEquals(1L, userWiseCounts.get("Priya Verma"));
    }

    @Test
    void runAllReportsWorkflow_FetchesAllCompletedCardsWithoutDateFilter() {
        VisitingCard card1 = VisitingCard.builder()
                .id(1L)
                .recordId("REC-001")
                .ocrStatus(OcrStatus.COMPLETED)
                .uploaderName("User 1")
                .build();
        VisitingCard card2 = VisitingCard.builder()
                .id(2L)
                .recordId("REC-002")
                .ocrStatus(OcrStatus.COMPLETED)
                .uploaderName("User 2")
                .build();

        when(visitingCardRepository.findByOcrStatus(OcrStatus.COMPLETED)).thenReturn(List.of(card1, card2));

        byte[] fakeExcel = new byte[]{4, 5, 6};
        when(excelReportService.generateReport(any(), eq("Consolidated Records OCR Report (Completed)"), any(), any(), any())).thenReturn(fakeExcel);
        when(emailService.sendReport(eq("Consolidated Reports"), eq(fakeExcel), any(), any(), any(), any(), any(), any(), any())).thenReturn("SUCCESS: Email delivered");

        Map<String, Object> result = scheduler.runAllReportsWorkflow();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals("Consolidated Reports", result.get("reportType"));
        assertEquals("ocr_status = COMPLETED", result.get("filter"));
        assertEquals(2L, result.get("totalRecords"));
        assertEquals("YES", result.get("emailSent"));

        @SuppressWarnings("unchecked")
        Map<String, Long> userWiseCounts = (Map<String, Long>) result.get("userWiseCounts");
        assertNotNull(userWiseCounts);
        assertEquals(1L, userWiseCounts.get("User 1"));
        assertEquals(1L, userWiseCounts.get("User 2"));
    }
}
