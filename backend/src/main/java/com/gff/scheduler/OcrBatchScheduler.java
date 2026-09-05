package com.gff.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scheduled job to run automated OCR extraction on pending visiting cards
 * and dispatch daily Excel report email to team leads every evening at 9:30 PM (21:30 IST).
 */
@Component
public class OcrBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcrBatchScheduler.class);

    private final DailyOcrReportScheduler dailyOcrReportScheduler;

    public OcrBatchScheduler(DailyOcrReportScheduler dailyOcrReportScheduler) {
        this.dailyOcrReportScheduler = dailyOcrReportScheduler;
    }

    /**
     * Cron expression: Second 0, Minute 30, Hour 21 (9:30 PM IST) every day.
     * Workflow:
     * 1. Completes any pending/processing OCR tasks dynamically (zero hardcoding).
     * 2. Fetches today's uploaded documents.
     * 3. Generates the Excel spreadsheet (.xlsx) with Apache POI.
     * 4. Emails the Excel attachment to team leads via Spring Boot Mail.
     */
    @Scheduled(cron = "${ocr.scheduler.cron:0 30 21 * * ?}", zone = "${ocr.scheduler.zone:Asia/Kolkata}")
    public void runEveningOcrBatch() {
        log.info("⏰ === Starting 9:30 PM Visiting Card OCR Batch & Email Report Job ===");
        Map<String, Object> result = dailyOcrReportScheduler.runDailyReportWorkflow();
        log.info("=== Completed 9:30 PM Visiting Card OCR Batch & Email Report Job. Result: {} ===", result);
    }
}
