package com.gff.scheduler;

import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.repository.VisitingCardRepository;
import com.gff.service.DynamicOcrService;
import com.gff.service.EmailService;
import com.gff.service.ExcelReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automated Cron Job Scheduler that executes the complete 4-step daily audit workflow:
 * 1. Resolves and completes any pending/processing OCR tasks.
 * 2. Queries today's complete document list.
 * 3. Generates an Excel spreadsheet (.xlsx) with Apache POI.
 * 4. Dispatches the report as an email attachment to team leads.
 */
@Component
public class DailyOcrReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyOcrReportScheduler.class);

    private final VisitingCardRepository visitingCardRepository;
    private final DynamicOcrService dynamicOcrService;
    private final ExcelReportService excelReportService;
    private final EmailService emailService;

    // Concurrency lock to prevent concurrent executions across threads/instances
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
     * Executes the complete 4-step workflow synchronously.
     * Invoked by OcrBatchScheduler at 9:30 PM or via Admin manual trigger endpoints.
     *
     * @return Map with execution summary, counts, and email dispatch status
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
            log.info("================================================================================");
            log.info(" STEP 1: Process Pending & In-Flight OCR Tasks");
            log.info("================================================================================");
            int resolvedCount = processPendingOcrTasks();

            log.info("================================================================================");
            log.info(" STEP 2: Fetch Complete List of Documents Uploaded for Today");
            log.info("================================================================================");
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = LocalDateTime.now();

            List<VisitingCard> todayCards = visitingCardRepository.findByCreatedAtBetween(startOfDay, endOfDay);
            if (todayCards.isEmpty()) {
                log.info("No documents uploaded strictly today ({}), fetching all available documents for audit report", today);
                todayCards = visitingCardRepository.findAll();
            }
            log.info("Found {} documents for today's daily audit report", todayCards.size());

            // Calculate metrics
            long total = todayCards.size();
            long completed = todayCards.stream().filter(c -> c.getOcrStatus() == OcrStatus.COMPLETED).count();
            long failed = todayCards.stream().filter(c -> c.getOcrStatus() == OcrStatus.FAILED).count();
            double successRate = total > 0 ? ((double) completed / total) * 100.0 : 100.0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total", total);
            stats.put("completed", completed);
            stats.put("failed", failed);
            stats.put("successRate", successRate);

            log.info("================================================================================");
            log.info(" STEP 3: Generate Excel (.xlsx) Spreadsheet with Apache POI");
            log.info("================================================================================");
            byte[] excelBytes = excelReportService.generateDailyOcrReport(todayCards, today, stats);
            log.info("Excel spreadsheet generated successfully (size: {} bytes)", excelBytes.length);

            log.info("================================================================================");
            log.info(" STEP 4: Email Spreadsheet Attachment to Team Leads via Spring Boot Mail");
            log.info("================================================================================");
            String emailStatus = emailService.sendDailyOcrReport(excelBytes, today, stats, todayCards);
            log.info("Email dispatch status: {}", emailStatus);

            result.put("status", "SUCCESS");
            result.put("reportDate", today.toString());
            result.put("pendingTasksResolved", resolvedCount);
            result.put("totalDocumentsInReport", total);
            result.put("metrics", stats);
            result.put("emailStatus", emailStatus);
            return result;

        } catch (Exception e) {
            log.error("Error executing Daily OCR Report Workflow: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
            return result;
        } finally {
            isJobRunning.set(false);
            log.info("Daily OCR Report Workflow completed. Concurrency lock released.");
        }
    }

    /**
     * Resolves and extracts any cards remaining in PENDING or PROCESSING state dynamically.
     */
    private int processPendingOcrTasks() {
        List<VisitingCard> pendingCards = visitingCardRepository.findByOcrStatusIn(
                List.of(OcrStatus.PENDING, OcrStatus.PROCESSING)
        );

        log.info("Found {} pending/processing visiting cards in database", pendingCards.size());
        int resolved = 0;

        for (VisitingCard card : pendingCards) {
            try {
                log.info("Processing OCR for card: {} (uploader: {})", card.getRecordId(), card.getUploaderEmail());
                card.setOcrStatus(OcrStatus.PROCESSING);
                visitingCardRepository.save(card);

                boolean success = dynamicOcrService.processCardOcrDynamically(card);
                if (success) {
                    resolved++;
                    log.info("Successfully completed dynamic OCR for card: {} (Holder: {})",
                            card.getRecordId(), card.getCardHolderName());
                } else {
                    log.warn("Dynamic OCR extraction failed or document unreadable for card: {}", card.getRecordId());
                }

            } catch (Exception e) {
                log.error("Failed to process OCR for card {}: {}", card.getRecordId(), e.getMessage());
                card.setOcrStatus(OcrStatus.FAILED);
                card.setErrorMessage(e.getMessage());
                visitingCardRepository.save(card);
            }
        }
        return resolved;
    }
}
