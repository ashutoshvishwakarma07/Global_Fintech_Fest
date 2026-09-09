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
        try {
            Map<String, Object> result = dailyOcrReportScheduler.runTodayReportWorkflow();
            if ("ERROR".equals(result.get("status"))) {
                return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow executed with notices", result));
            }
            return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow triggered successfully", result));
        } catch (Throwable t) {
            Map<String, Object> errResult = Map.of("status", "ERROR", "errorMessage", t.getMessage() != null ? t.getMessage() : t.toString());
            return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow completed with warning", errResult));
        }
    }

    @GetMapping("/trigger-today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTodayReportGet() {
        return triggerTodayReportPost();
    }

    /**
     * Consolidated Reports trigger endpoint:
     * Queries all records where ocr_status = COMPLETED (no date filter), aggregates user-wise counts, generates Excel, and emails report.
     *
     * URL: POST /api/v1/admin/reports/trigger-all
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAllReportsPost() {
        try {
            Map<String, Object> result = dailyOcrReportScheduler.runAllReportsWorkflow();
            if ("ERROR".equals(result.get("status"))) {
                return ResponseEntity.ok(ApiResponse.success("Consolidated Reports OCR Workflow executed with notices", result));
            }
            return ResponseEntity.ok(ApiResponse.success("Consolidated Reports OCR Workflow triggered successfully", result));
        } catch (Throwable t) {
            Map<String, Object> errResult = Map.of("status", "ERROR", "errorMessage", t.getMessage() != null ? t.getMessage() : t.toString());
            return ResponseEntity.ok(ApiResponse.success("Consolidated Reports OCR Workflow completed with warning", errResult));
        }
    }

    @GetMapping("/trigger-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAllReportsGet() {
        return triggerAllReportsPost();
    }

    /**
     * Legacy daily trigger alias (for backwards compatibility).
     * URL: POST /api/v1/admin/reports/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportPost() {
        return triggerTodayReportPost();
    }

    @GetMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportGet() {
        return triggerTodayReportPost();
    }
}
