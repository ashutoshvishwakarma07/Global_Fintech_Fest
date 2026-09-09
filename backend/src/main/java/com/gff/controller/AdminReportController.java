package com.gff.controller;

import com.gff.dto.response.ApiResponse;
import com.gff.scheduler.DailyOcrReportScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for Administrative Reports and on-demand trigger operations.
 */
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final DailyOcrReportScheduler dailyOcrReportScheduler;

    public AdminReportController(DailyOcrReportScheduler dailyOcrReportScheduler) {
        this.dailyOcrReportScheduler = dailyOcrReportScheduler;
    }

    /**
     * Today's Report trigger endpoint:
     * Queries only today's uploaded records, aggregates user-wise counts, generates Excel, and emails report.
     *
     * URL: POST /api/v1/admin/reports/trigger-today
     */
    @PostMapping("/trigger-today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTodayReportPost() {
        Map<String, Object> result = dailyOcrReportScheduler.runTodayReportWorkflow();
        return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow triggered successfully", result));
    }

    @GetMapping("/trigger-today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTodayReportGet() {
        Map<String, Object> result = dailyOcrReportScheduler.runTodayReportWorkflow();
        return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow triggered successfully", result));
    }

    /**
     * All Reports trigger endpoint:
     * Queries all records where ocr_status = COMPLETED (no date filter), aggregates user-wise counts, generates Excel, and emails report.
     *
     * URL: POST /api/v1/admin/reports/trigger-all
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAllReportsPost() {
        Map<String, Object> result = dailyOcrReportScheduler.runAllReportsWorkflow();
        return ResponseEntity.ok(ApiResponse.success("All Reports OCR Workflow triggered successfully", result));
    }

    @GetMapping("/trigger-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAllReportsGet() {
        Map<String, Object> result = dailyOcrReportScheduler.runAllReportsWorkflow();
        return ResponseEntity.ok(ApiResponse.success("All Reports OCR Workflow triggered successfully", result));
    }

    /**
     * Legacy daily trigger alias (for backwards compatibility).
     * URL: POST /api/v1/admin/reports/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportPost() {
        Map<String, Object> result = dailyOcrReportScheduler.runTodayReportWorkflow();
        return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow triggered successfully", result));
    }

    @GetMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportGet() {
        Map<String, Object> result = dailyOcrReportScheduler.runTodayReportWorkflow();
        return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow triggered successfully", result));
    }
}
