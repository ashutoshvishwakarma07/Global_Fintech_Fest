package com.gff.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scheduled job to generate Consolidated OCR report
 * and dispatch Excel report email to team leads every evening at 7:00 PM IST (19:00 IST).
 */
@Component
public class OcrBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcrBatchScheduler.class);

    private final DailyOcrReportScheduler dailyOcrReportScheduler;

    public OcrBatchScheduler(DailyOcrReportScheduler dailyOcrReportScheduler) {
        this.dailyOcrReportScheduler = dailyOcrReportScheduler;
    }

    /**
     * Cron expression: Second 0, Minute 0, Hour 19 (07:00 PM IST) every day.
     * Uses Asia/Kolkata timezone so execution is strictly independent of EC2 server local time.
     */
    @Scheduled(cron = "${ocr.scheduler.cron:0 0 19 * * *}", zone = "${ocr.scheduler.zone:Asia/Kolkata}")
    public void runEveningOcrBatch() {
        log.info("⏰ [07:00 PM IST] Triggering Consolidated OCR Report Scheduled Batch Job...");
        Map<String, Object> result = dailyOcrReportScheduler.runAllReportsWorkflow();
        log.info("⏰ [07:00 PM IST] Consolidated Report Batch finished with result: {}", result);
    }

    /**
     * Backward-compatible trigger method for controller manual endpoints.
     */
    public Map<String, Object> triggerBatchManually() {
        log.info("⏰ Manual Trigger: Executing Consolidated Report Job...");
        return dailyOcrReportScheduler.runAllReportsWorkflow();
    }
}
