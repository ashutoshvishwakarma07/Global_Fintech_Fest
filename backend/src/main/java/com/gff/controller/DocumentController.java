package com.gff.controller;

import com.gff.config.JwtUtil;
import com.gff.dto.request.DocumentUploadRequest;
import com.gff.dto.response.ApiResponse;
import com.gff.dto.response.DashboardStatsResponse;
import com.gff.dto.response.DocumentResponse;
import com.gff.entity.enums.RecordStatus;
import com.gff.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gff.scheduler.OcrBatchScheduler;
import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final JwtUtil jwtUtil;
    private final OcrBatchScheduler ocrBatchScheduler;

    public DocumentController(DocumentService documentService, JwtUtil jwtUtil, OcrBatchScheduler ocrBatchScheduler) {
        this.documentService = documentService;
        this.jwtUtil = jwtUtil;
        this.ocrBatchScheduler = ocrBatchScheduler;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Valid @RequestBody DocumentUploadRequest request) {
        DocumentResponse response = documentService.createUploadRecord(request);
        return new ResponseEntity<>(ApiResponse.success("Document uploaded successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String email = headerEmail;
        String role = headerRole;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Map<String, Object> claims = jwtUtil.validateAndExtractClaims(token);
            if (claims != null) {
                email = (String) claims.get("sub");
                role = (String) claims.get("role");
            }
        }

        Page<DocumentResponse> records = documentService.getDocuments(email, role, query, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", records));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        String email = headerEmail;
        String role = headerRole;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Map<String, Object> claims = jwtUtil.validateAndExtractClaims(token);
            if (claims != null) {
                email = (String) claims.get("sub");
                role = (String) claims.get("role");
            }
        }

        DocumentResponse response = documentService.getDocumentById(id, email, role);
        return ResponseEntity.ok(ApiResponse.success("Document found", response));
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentByRecordId(
            @PathVariable String recordId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {

        String email = headerEmail;
        String role = headerRole;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Map<String, Object> claims = jwtUtil.validateAndExtractClaims(token);
            if (claims != null) {
                email = (String) claims.get("sub");
                role = (String) claims.get("role");
            }
        }

        DocumentResponse response = documentService.getDocumentByRecordId(recordId, email, role);
        return ResponseEntity.ok(ApiResponse.success("Document found", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = documentService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", stats));
    }

    @GetMapping("/trigger-ocr-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerOcrBatchGet() {
        ocrBatchScheduler.runEveningOcrBatch();
        return ResponseEntity.ok(ApiResponse.success("9:30 PM OCR Batch job triggered successfully",
                Map.of("status", "SUCCESS", "message", "Processed all pending visiting cards and generated daily report")));
    }

    @PostMapping("/trigger-ocr-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerOcrBatchPost() {
        ocrBatchScheduler.runEveningOcrBatch();
        return ResponseEntity.ok(ApiResponse.success("9:30 PM OCR Batch job triggered successfully",
                Map.of("status", "SUCCESS", "message", "Processed all pending visiting cards and generated daily report")));
    }
}
