package com.gff.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scheduled job to run automated OCR extraction on pending visiting cards
 * and dispatch daily Excel report email to team leads every morning at 7:00 AM IST.
 */
@Component
public class OcrBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcrBatchScheduler.class);

    private final DailyOcrReportScheduler dailyOcrReportScheduler;

    public OcrBatchScheduler(DailyOcrReportScheduler dailyOcrReportScheduler) {
        this.dailyOcrReportScheduler = dailyOcrReportScheduler;
    }

    /**
     * Cron expression: Second 0, Minute 0, Hour 7 (07:00 AM IST) every day.
     * Uses Asia/Kolkata timezone so execution is strictly independent of EC2 server local time.
     */
    @Scheduled(cron = "${ocr.scheduler.cron:0 0 7 * * *}", zone = "${ocr.scheduler.zone:Asia/Kolkata}")
    public void runMorningOcrBatch() {
        log.info("⏰ [07:00 AM IST] Triggering Daily Scheduled OCR & Export Batch Job...");
        Map<String, Object> result = dailyOcrReportScheduler.runDailyReportWorkflow();
        log.info("⏰ [07:00 AM IST] Daily Scheduled OCR Batch finished with result: {}", result);
    }

    /**
     * Backward-compatible trigger method for controller manual endpoints.
     */
    public Map<String, Object> runEveningOcrBatch() {
        log.info("⏰ Manual Trigger: Executing Visiting Card Daily OCR & Report Job...");
        return dailyOcrReportScheduler.runDailyReportWorkflow();
    }
}
