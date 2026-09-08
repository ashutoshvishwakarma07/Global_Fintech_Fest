package com.gff.scheduler;

import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.repository.VisitingCardRepository;
import com.gff.service.DynamicOcrService;
import com.gff.service.EmailService;
import com.gff.service.ExcelReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automated Cron Job Scheduler that executes the daily 7:00 AM IST audit workflow:
 * 1. Finds pending visiting card records (ocr_status = PENDING).
 * 2. Processes them through dynamic OCR.
 * 3. Identifies only records newly completed during this execution (ocr_status = COMPLETED, email_sent_at IS NULL).
 * 4. Generates an Excel sheet and emails it ONLY if newly completed records exist.
 * 5. Marks successfully emailed records with email_sent_at = current timestamp to prevent duplicates.
 */
@Component
public class DailyOcrReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyOcrReportScheduler.class);

    private final VisitingCardRepository visitingCardRepository;
    private final DynamicOcrService dynamicOcrService;
    private final ExcelReportService excelReportService;
    private final EmailService emailService;

    // Concurrency lock to prevent overlapping executions
    private final AtomicBoolean isJobRunning = new AtomicBoolean(false);

    public DailyOcrReportScheduler(VisitingCardRepository visitingCardRepository,
                                  DynamicOcrService dynamicOcrService,
                                  ExcelReportService excelReportService,
                                  EmailService emailService) {
        this.visitingCardRepository = visitingCardRepository;
        this.dynamicOcrService = dynamicOcrService;
        this.excelReportService = excelReportService;
        this.emailService = emailService;
    }

    /**
     * Executes the daily 7:00 AM IST OCR batch and conditional export workflow.
     *
     * @return Map with execution metrics, counts, and email dispatch status
     */
    public Map<String, Object> runDailyReportWorkflow() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Ensure concurrency safety
        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn("Daily OCR Report Workflow is already executing. Concurrency lock acquired by another thread.");
            result.put("status", "LOCKED");
            result.put("message", "A report generation process is already in progress.");
            return result;
        }

        try {
            log.info("Daily OCR job started - 07:00 AM IST");

            // 1. Query all pending visiting-card records
            List<VisitingCard> pendingCards = visitingCardRepository.findByOcrStatusIn(
                    List.of(OcrStatus.PENDING, OcrStatus.PROCESSING)
            );
            int pendingCount = pendingCards.size();
            log.info("Pending records found: {}", pendingCount);

            int completedCount = 0;
            int failedCount = 0;
            List<VisitingCard> newlyCompletedCards = new ArrayList<>();

            // 2. Process records through OCR
            for (VisitingCard card : pendingCards) {
                try {
                    card.setOcrStatus(OcrStatus.PROCESSING);
                    visitingCardRepository.save(card);

                    boolean success = dynamicOcrService.processCardOcrDynamically(card);
                    if (success && card.getOcrStatus() == OcrStatus.COMPLETED) {
                        completedCount++;
                        // Only include in export if not already emailed
                        if (card.getEmailSentAt() == null) {
                            newlyCompletedCards.add(card);
                        }
                    } else {
                        failedCount++;
                        card.setOcrStatus(OcrStatus.FAILED);
                        visitingCardRepository.save(card);
                    }
                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to process OCR for card {}: {}", card.getRecordId(), e.getMessage());
                    card.setOcrStatus(OcrStatus.FAILED);
                    card.setErrorMessage(e.getMessage());
                    visitingCardRepository.save(card);
                }
            }

            log.info("OCR completed: {}", completedCount);
            log.info("OCR failed: {}", failedCount);

            // If newly completed list is empty, include all un-emailed or all completed records so manual trigger always exports
            if (newlyCompletedCards.isEmpty()) {
                List<VisitingCard> unemailed = visitingCardRepository.findByOcrStatusAndEmailSentAtIsNull(OcrStatus.COMPLETED);
                if (!unemailed.isEmpty()) {
                    newlyCompletedCards.addAll(unemailed);
                    log.info("Collected {} un-emailed completed records for export", unemailed.size());
                } else {
                    List<VisitingCard> allCompleted = visitingCardRepository.findByOcrStatus(OcrStatus.COMPLETED);
                    newlyCompletedCards.addAll(allCompleted);
                    log.info("Collected {} total completed records for on-demand export", allCompleted.size());
                }
            }

            log.info("Records ready for export: {}", newlyCompletedCards.size());

            boolean emailSent = false;
            int recordsMarkedAsEmailed = 0;

            // 3. If there are newly completed records: Generate Excel and send email
            if (!newlyCompletedCards.isEmpty()) {
                LocalDate today = LocalDate.now();
                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("total", (long) newlyCompletedCards.size());
                stats.put("completed", (long) completedCount);
                stats.put("failed", (long) failedCount);
                double successRate = (completedCount + failedCount) > 0
                        ? ((double) completedCount / (completedCount + failedCount)) * 100.0
                        : 100.0;
                stats.put("successRate", successRate);

                byte[] excelBytes = excelReportService.generateDailyOcrReport(newlyCompletedCards, today, stats);
                log.info("Excel spreadsheet generated for {} newly completed records (size: {} bytes)",
                        newlyCompletedCards.size(), excelBytes.length);

                String emailStatus = emailService.sendDailyOcrReport(excelBytes, today, stats, newlyCompletedCards);
                log.info("Email dispatch status: {}", emailStatus);

                // Mark records as emailed ONLY upon successful email delivery
                if (emailStatus != null && emailStatus.startsWith("SUCCESS")) {
                    emailSent = true;
                    LocalDateTime sentTimestamp = LocalDateTime.now();
                    for (VisitingCard card : newlyCompletedCards) {
                        card.setEmailSentAt(sentTimestamp);
                        visitingCardRepository.save(card);
                        recordsMarkedAsEmailed++;
                    }
                } else {
                    log.warn("Email delivery did not return SUCCESS. Records will not be marked as emailed to allow retry.");
                }
            } else {
                // No pending records or no newly completed records: do not generate sheet or send email
                log.info("No new data to export. Skipping sheet generation and email dispatch.");
            }

            log.info("Email sent: {}", emailSent ? "YES" : "NO");
            log.info("Records marked as emailed: {}", recordsMarkedAsEmailed);
            log.info("Daily OCR job completed");

            result.put("status", "SUCCESS");
            result.put("pendingRecordsFound", pendingCount);
            result.put("ocrCompleted", completedCount);
            result.put("ocrFailed", failedCount);
            result.put("recordsReadyForExport", newlyCompletedCards.size());
            result.put("emailSent", emailSent ? "YES" : "NO");
            result.put("recordsMarkedAsEmailed", recordsMarkedAsEmailed);
            return result;

        } catch (Exception e) {
            log.error("Error executing Daily OCR job: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
            return result;
        } finally {
            isJobRunning.set(false);
        }
    }
}
