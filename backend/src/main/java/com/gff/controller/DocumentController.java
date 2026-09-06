package com.gff.controller;

import com.gff.dto.request.DocumentUploadRequest;
import com.gff.dto.response.ApiResponse;
import com.gff.dto.response.DashboardStatsResponse;
import com.gff.dto.response.DocumentResponse;
import com.gff.entity.User;
import com.gff.entity.enums.RecordStatus;
import com.gff.exception.ApiException;
import com.gff.service.DocumentService;
import com.gff.scheduler.OcrBatchScheduler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final OcrBatchScheduler ocrBatchScheduler;

    public DocumentController(DocumentService documentService, OcrBatchScheduler ocrBatchScheduler) {
        this.documentService = documentService;
        this.ocrBatchScheduler = ocrBatchScheduler;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Valid @RequestBody DocumentUploadRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = (User) httpRequest.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ApiException("Full authentication is required to upload documents", HttpStatus.UNAUTHORIZED);
        }

        DocumentResponse response = documentService.createUploadRecord(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Document uploaded successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        User currentUser = (User) httpRequest.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ApiException("Full authentication is required to view documents", HttpStatus.UNAUTHORIZED);
        }

        Page<DocumentResponse> records = documentService.getDocuments(
                currentUser.getEmail(),
                currentUser.getRole().name(),
                query,
                status,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", records));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        User currentUser = (User) httpRequest.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ApiException("Full authentication is required to view document details", HttpStatus.UNAUTHORIZED);
        }

        DocumentResponse record = documentService.getDocumentById(
                id,
                currentUser.getEmail(),
                currentUser.getRole().name()
        );
        return ResponseEntity.ok(ApiResponse.success("Document details retrieved", record));
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentByRecordId(
            @PathVariable String recordId,
            HttpServletRequest httpRequest) {

        User currentUser = (User) httpRequest.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ApiException("Full authentication is required to view document details", HttpStatus.UNAUTHORIZED);
        }

        DocumentResponse record = documentService.getDocumentByRecordId(
                recordId,
                currentUser.getEmail(),
                currentUser.getRole().name()
        );
        return ResponseEntity.ok(ApiResponse.success("Document details retrieved", record));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(HttpServletRequest httpRequest) {
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ApiException("Full authentication is required to view dashboard statistics", HttpStatus.UNAUTHORIZED);
        }

        DashboardStatsResponse stats = documentService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved", stats));
    }

    /**
     * Admin manual trigger for 11:15 PM OCR batch job.
     */
    @GetMapping("/trigger-ocr-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerOcrBatchManually() {
        ocrBatchScheduler.runEveningOcrBatch();
        return ResponseEntity.ok(ApiResponse.success(
                "Visiting Card OCR Batch & Email Report triggered successfully",
                Map.of("status", "SUCCESS", "message", "Processed all pending visiting cards and generated daily report")
        ));
    }
}
