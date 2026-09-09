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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Scheduler and On-Demand Workflow Engine for Administrative Reports:
 * 1. Today's Report: Queries today's uploaded records, aggregates user-wise counts, generates Excel, and emails report.
 * 2. All Reports: Queries all records where ocr_status = COMPLETED (no date filter), aggregates user-wise counts, generates Excel, and emails report.
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
     * Legacy & scheduled cron runner: delegates to Today's Report workflow.
     */
    public Map<String, Object> runDailyReportWorkflow() {
        return runTodayReportWorkflow();
    }

    /**
     * "Today's Report" Workflow:
     * 1. Queries only today's uploaded records from the database.
     * 2. Automatically processes any pending OCR on today's items.
     * 3. Calculates user-wise upload counts (e.g. User 1 – 10, User 2 – 20, Total – 30).
     * 4. Generates a formatted Excel report (.xlsx).
     * 5. Dispatches email with Excel attachment and user-wise summary.
     */
    public Map<String, Object> runTodayReportWorkflow() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn("Report generation is already executing. Concurrency lock active.");
            result.put("status", "LOCKED");
            result.put("message", "A report generation process is already in progress.");
            return result;
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
            String dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            log.info("Starting [Today's Report] workflow for date: {}", dateStr);

            // 1. Fetch only records created today
            List<VisitingCard> todayCards = visitingCardRepository.findByCreatedAtBetween(startOfDay, endOfDay);
            log.info("Total records uploaded today: {}", todayCards.size());

            // Process any pending OCR for today's cards
            int pendingCount = 0;
            int completedCount = 0;
            int failedCount = 0;

            for (VisitingCard card : todayCards) {
                if (card.getOcrStatus() == OcrStatus.PENDING || card.getOcrStatus() == OcrStatus.PROCESSING) {
                    pendingCount++;
                    try {
                        card.setOcrStatus(OcrStatus.PROCESSING);
                        visitingCardRepository.save(card);
                        boolean success = dynamicOcrService.processCardOcrDynamically(card);
                        if (success && card.getOcrStatus() == OcrStatus.COMPLETED) {
                            completedCount++;
                        } else {
                            failedCount++;
                            card.setOcrStatus(OcrStatus.FAILED);
                            visitingCardRepository.save(card);
                        }
                    } catch (Exception e) {
                        failedCount++;
                        log.error("OCR error on card {}: {}", card.getRecordId(), e.getMessage());
                        card.setOcrStatus(OcrStatus.FAILED);
                        card.setErrorMessage(e.getMessage());
                        visitingCardRepository.save(card);
                    }
                } else if (card.getOcrStatus() == OcrStatus.COMPLETED) {
                    completedCount++;
                } else if (card.getOcrStatus() == OcrStatus.FAILED) {
                    failedCount++;
                }
            }

            // If no records were uploaded today, include any unemailed completed cards as a fallback
            if (todayCards.isEmpty()) {
                List<VisitingCard> unemailed = visitingCardRepository.findByOcrStatusAndEmailSentAtIsNull(OcrStatus.COMPLETED);
                if (!unemailed.isEmpty()) {
                    todayCards = unemailed;
                    completedCount = unemailed.size();
                    log.info("No records strictly timestamped today; loaded {} unemailed completed records as fallback", unemailed.size());
                }
            }

            // 2. Compute user-wise upload counts
            Map<String, Long> userWiseCounts = computeUserWiseCounts(todayCards);
            log.info("User-wise breakdown for Today's Report: {}", userWiseCounts);

            // 3. Stats calculation
            long total = todayCards.size();
            double successRate = total > 0 ? ((double) completedCount / total) * 100.0 : 100.0;
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total", total);
            stats.put("completed", (long) completedCount);
            stats.put("failed", (long) failedCount);
            stats.put("successRate", successRate);

            // 4. Generate Excel report
            String attachmentFileName = "Today_OCR_Report_" + dateStr + ".xlsx";
            byte[] excelBytes = excelReportService.generateReport(
                    todayCards,
                    "Today's OCR & Upload Summary",
                    today,
                    stats,
                    userWiseCounts
            );
            log.info("Generated Today's Report Excel workbook (size: {} bytes)", excelBytes.length);

            // 5. Send Email
            String emailStatus = emailService.sendReport(
                    "Today's Report",
                    excelBytes,
                    attachmentFileName,
                    today,
                    stats,
                    userWiseCounts,
                    todayCards
            );
            log.info("Today's Report email status: {}", emailStatus);

            boolean emailSent = emailStatus != null && emailStatus.startsWith("SUCCESS");
            if (emailSent) {
                LocalDateTime now = LocalDateTime.now();
                for (VisitingCard card : todayCards) {
                    card.setEmailSentAt(now);
                    visitingCardRepository.save(card);
                }
            }

            result.put("status", "SUCCESS");
            result.put("reportType", "Today's Report");
            result.put("reportDate", dateStr);
            result.put("totalRecords", total);
            result.put("completedRecords", completedCount);
            result.put("failedRecords", failedCount);
            result.put("userWiseCounts", userWiseCounts);
            result.put("emailSent", emailSent ? "YES" : "NO");
            result.put("emailStatus", emailStatus);
            result.put("attachmentName", attachmentFileName);
            return result;

        } catch (Exception e) {
            log.error("Error executing Today's Report workflow: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
            return result;
        } finally {
            isJobRunning.set(false);
        }
    }

    /**
     * "All Reports" Workflow:
     * 1. Fetches all uploaded records from the database without any date filter,
     *    with filter: ocr_status = COMPLETED.
     * 2. Computes user-wise upload counts across all completed records.
     * 3. Generates Excel report (.xlsx) containing all completed records.
     * 4. Dispatches email with Excel attachment and user-wise summary.
     */
    public Map<String, Object> runAllReportsWorkflow() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn("Report generation is already executing. Concurrency lock active.");
            result.put("status", "LOCKED");
            result.put("message", "A report generation process is already in progress.");
            return result;
        }

        try {
            LocalDate today = LocalDate.now();
            String dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            log.info("Starting [All Reports] workflow (filter: ocr_status = COMPLETED, no date filter)");

            // 1. Fetch all records with OCR status COMPLETED across all time
            List<VisitingCard> completedCards = visitingCardRepository.findByOcrStatus(OcrStatus.COMPLETED);
            log.info("Total completed records found in database: {}", completedCards.size());

            // 2. Compute user-wise upload counts
            Map<String, Long> userWiseCounts = computeUserWiseCounts(completedCards);
            log.info("User-wise breakdown for All Reports: {}", userWiseCounts);

            // 3. Stats calculation
            long total = completedCards.size();
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total", total);
            stats.put("completed", total);
            stats.put("failed", 0L);
            stats.put("successRate", 100.0);

            // 4. Generate Excel report
            String attachmentFileName = "All_OCR_Report_" + dateStr + ".xlsx";
            byte[] excelBytes = excelReportService.generateReport(
                    completedCards,
                    "All Records OCR Report (Completed)",
                    today,
                    stats,
                    userWiseCounts
            );
            log.info("Generated All Reports Excel workbook for {} records (size: {} bytes)", completedCards.size(), excelBytes.length);

            // 5. Send Email
            String emailStatus = emailService.sendReport(
                    "All Reports",
                    excelBytes,
                    attachmentFileName,
                    today,
                    stats,
                    userWiseCounts,
                    completedCards
            );
            log.info("All Reports email status: {}", emailStatus);

            boolean emailSent = emailStatus != null && emailStatus.startsWith("SUCCESS");

            result.put("status", "SUCCESS");
            result.put("reportType", "All Reports");
            result.put("filter", "ocr_status = COMPLETED");
            result.put("totalRecords", total);
            result.put("userWiseCounts", userWiseCounts);
            result.put("emailSent", emailSent ? "YES" : "NO");
            result.put("emailStatus", emailStatus);
            result.put("attachmentName", attachmentFileName);
            return result;

        } catch (Exception e) {
            log.error("Error executing All Reports workflow: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
            return result;
        } finally {
            isJobRunning.set(false);
        }
    }

    /**
     * Helper to compute user-wise aggregation counts.
     */
    private Map<String, Long> computeUserWiseCounts(List<VisitingCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyMap();
        }
        return cards.stream().collect(
                Collectors.groupingBy(
                        card -> {
                            if (card.getUploaderName() != null && !card.getUploaderName().isBlank()) {
                                return card.getUploaderName();
                            } else if (card.getUploaderEmail() != null && !card.getUploaderEmail().isBlank()) {
                                return card.getUploaderEmail();
                            } else {
                                return "Unknown User";
                            }
                        },
                        LinkedHashMap::new,
                        Collectors.counting()
                )
        );
    }
}
