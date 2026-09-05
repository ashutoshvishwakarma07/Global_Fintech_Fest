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
     * Manual trigger endpoint for Admins to test the complete 4-step workflow:
     * 1. Process pending OCR
     * 2. Query today's documents
     * 3. Generate Excel spreadsheet
     * 4. Email report to team leads
     *
     * URL: POST /api/v1/admin/reports/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportPost() {
        Map<String, Object> result = dailyOcrReportScheduler.runDailyReportWorkflow();
        return ResponseEntity.ok(ApiResponse.success("Daily OCR Report Workflow triggered successfully", result));
    }

    /**
     * GET alternative for browser testing.
     * URL: GET /api/v1/admin/reports/trigger-daily
     */
    @GetMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportGet() {
        Map<String, Object> result = dailyOcrReportScheduler.runDailyReportWorkflow();
        return ResponseEntity.ok(ApiResponse.success("Daily OCR Report Workflow triggered successfully", result));
    }
}
