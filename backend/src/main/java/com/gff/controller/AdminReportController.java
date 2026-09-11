package com.gff.controller;

import com.gff.dto.request.SendReportRequest;
import com.gff.dto.response.ApiResponse;
import com.gff.scheduler.DailyOcrReportScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Controller for Administrative Reports and on-demand trigger operations.
 * Supports configurable dynamic To and CC email recipients.
 */
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {

    private static final Logger log = LoggerFactory.getLogger(AdminReportController.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTodayReportPost(
            @RequestBody(required = false) SendReportRequest request) {
        try {
            List<String> rawTo = (request != null && request.getTo() != null) ? request.getTo() : List.of();
            List<String> rawCc = (request != null && request.getCc() != null) ? request.getCc() : List.of();

            List<String> toList = sanitizeAndValidateEmails(rawTo);
            List<String> ccList = sanitizeAndValidateEmails(rawCc);

            if (request != null && (request.getTo() != null || request.getCc() != null)) {
                if (toList.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error("Validation Failed: At least one valid recipient (To) email is required.")
                    );
                }
            }

            log.info("Triggering Today's OCR Report email to TO: {}, CC: {}", toList, ccList);
            Map<String, Object> result = dailyOcrReportScheduler.runTodayReportWorkflow(toList, ccList);
            if ("ERROR".equals(result.get("status"))) {
                return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow executed with notices", result));
            }
            return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow triggered successfully", result));
        } catch (Throwable t) {
            log.error("Failed to trigger Today's OCR Report: {}", t.getMessage(), t);
            Map<String, Object> errResult = Map.of("status", "ERROR", "errorMessage", t.getMessage() != null ? t.getMessage() : t.toString());
            return ResponseEntity.ok(ApiResponse.success("Today's OCR Report Workflow completed with warning", errResult));
        }
    }

    @GetMapping("/trigger-today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTodayReportGet() {
        return triggerTodayReportPost(null);
    }

    /**
     * Consolidated Reports trigger endpoint:
     * Queries all records where ocr_status = COMPLETED (no date filter), aggregates user-wise counts, generates Excel, and emails report.
     *
     * URL: POST /api/v1/admin/reports/trigger-all
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAllReportsPost(
            @RequestBody(required = false) SendReportRequest request) {
        try {
            List<String> rawTo = (request != null && request.getTo() != null) ? request.getTo() : List.of();
            List<String> rawCc = (request != null && request.getCc() != null) ? request.getCc() : List.of();

            List<String> toList = sanitizeAndValidateEmails(rawTo);
            List<String> ccList = sanitizeAndValidateEmails(rawCc);

            if (request != null && (request.getTo() != null || request.getCc() != null)) {
                if (toList.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error("Validation Failed: At least one valid recipient (To) email is required.")
                    );
                }
            }

            log.info("Triggering Consolidated Reports OCR email to TO: {}, CC: {}", toList, ccList);
            Map<String, Object> result = dailyOcrReportScheduler.runAllReportsWorkflow(toList, ccList);
            if ("ERROR".equals(result.get("status"))) {
                return ResponseEntity.ok(ApiResponse.success("Consolidated Reports OCR Workflow executed with notices", result));
            }
            return ResponseEntity.ok(ApiResponse.success("Consolidated Reports OCR Workflow triggered successfully", result));
        } catch (Throwable t) {
            log.error("Failed to trigger Consolidated Reports: {}", t.getMessage(), t);
            Map<String, Object> errResult = Map.of("status", "ERROR", "errorMessage", t.getMessage() != null ? t.getMessage() : t.toString());
            return ResponseEntity.ok(ApiResponse.success("Consolidated Reports OCR Workflow completed with warning", errResult));
        }
    }

    @GetMapping("/trigger-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAllReportsGet() {
        return triggerAllReportsPost(null);
    }

    /**
     * Legacy daily trigger alias (for backwards compatibility).
     * URL: POST /api/v1/admin/reports/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportPost(
            @RequestBody(required = false) SendReportRequest request) {
        return triggerTodayReportPost(request);
    }

    @GetMapping("/trigger-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDailyReportGet() {
        return triggerTodayReportPost(null);
    }

    /**
     * Sanitizes, trims, deduplicates, and filters out invalid email formats.
     */
    private List<String> sanitizeAndValidateEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String raw : emails) {
            if (raw == null) continue;
            String trimmed = raw.trim().toLowerCase();
            if (!trimmed.isEmpty() && EMAIL_PATTERN.matcher(trimmed).matches()) {
                set.add(trimmed);
            }
        }
        return new ArrayList<>(set);
    }
}

