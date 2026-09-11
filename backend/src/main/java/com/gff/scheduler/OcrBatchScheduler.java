package com.gff.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Service to generate Consolidated OCR report
 * and dispatch Excel report email to team leads on demand.
 */
@Component
public class OcrBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcrBatchScheduler.class);

    private final DailyOcrReportScheduler dailyOcrReportScheduler;

    public OcrBatchScheduler(DailyOcrReportScheduler dailyOcrReportScheduler) {
        this.dailyOcrReportScheduler = dailyOcrReportScheduler;
    }

    /**
     * Executes Consolidated OCR report workflow on demand.
     */
    public void runEveningOcrBatch() {
        log.info("⏰ Triggering Consolidated OCR Report Batch Job...");
        Map<String, Object> result = dailyOcrReportScheduler.runAllReportsWorkflow();
        log.info("⏰ Consolidated Report Batch finished with result: {}", result);
    }

    /**
     * Backward-compatible trigger method for controller manual endpoints.
     */
    public Map<String, Object> triggerBatchManually() {
        log.info("⏰ Manual Trigger: Executing Consolidated Report Job...");
        return dailyOcrReportScheduler.runAllReportsWorkflow();
    }
}
