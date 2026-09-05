package com.gff.service;

import com.gff.dto.request.DocumentUploadRequest;
import com.gff.dto.response.DashboardStatsResponse;
import com.gff.dto.response.DocumentResponse;
import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.entity.enums.UserRole;
import com.gff.exception.ApiException;
import com.gff.exception.ResourceNotFoundException;
import com.gff.repository.VisitingCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final VisitingCardRepository visitingCardRepository;
    private final S3Service s3Service;

    public DocumentService(VisitingCardRepository visitingCardRepository, S3Service s3Service) {
        this.visitingCardRepository = visitingCardRepository;
        this.s3Service = s3Service;
    }

    @Transactional
    public DocumentResponse createUploadRecord(DocumentUploadRequest request) {
        final String finalRecordId = (request.getRecordId() != null && !request.getRecordId().trim().isEmpty())
                ? request.getRecordId()
                : "REC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5);

        if (visitingCardRepository.existsByRecordId(finalRecordId)) {
            VisitingCard existing = visitingCardRepository.findByRecordId(finalRecordId)
                    .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "recordId", finalRecordId));
            return DocumentResponse.fromEntity(existing);
        }

        String s3Key = null;
        String imageUrl = null;

        if (request.getImageBase64() != null && !request.getImageBase64().trim().isEmpty()) {
            try {
                String base64Str = request.getImageBase64();
                String contentType = "image/jpeg";
                if (base64Str.contains(",")) {
                    String prefix = base64Str.substring(0, base64Str.indexOf(","));
                    if (prefix.contains("image/png")) contentType = "image/png";
                    else if (prefix.contains("image/webp")) contentType = "image/webp";
                    base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
                }

                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Str.trim());
                s3Key = "visiting-cards/" + finalRecordId + ".jpg";
                imageUrl = s3Service.uploadDirectToS3(imageBytes, s3Key, contentType);
                log.info("Uploaded card photo to AWS S3: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload image to S3: {}", e.getMessage(), e);
            }
        }

        boolean hasOcr = request.getCardHolderName() != null && !request.getCardHolderName().trim().isEmpty();

        VisitingCard card = VisitingCard.builder()
                .recordId(finalRecordId)
                .uploaderName(request.getUploaderName())
                .uploaderEmail(request.getUploaderEmail())
                .uploaderMobile(request.getUploaderMobile())
                .uploaderRole(request.getUploaderRole() != null ? request.getUploaderRole() : UserRole.FIELD_USER)
                .fileName(request.getFileName() != null ? request.getFileName() : finalRecordId + ".jpg")
                .fileSize(request.getFileSize())
                .notes(request.getNotes())
                .imageUrl(imageUrl)
                .s3Key(s3Key)
                .s3Bucket("visiting-card-bkt")
                .status(Boolean.TRUE.equals(request.getIsOffline()) ? RecordStatus.PENDING_UPLOAD : RecordStatus.UPLOADED)
                .isOffline(request.getIsOffline() != null ? request.getIsOffline() : false)
                .cardHolderName(request.getCardHolderName())
                .companyName(request.getCompanyName())
                .designation(request.getDesignation())
                .extractedEmail(request.getExtractedEmail())
                .extractedMobile(request.getExtractedMobile())
                .extractedAddress(request.getExtractedAddress())
                .rawOcrText(request.getRawOcrText())
                .ocrStatus(hasOcr ? OcrStatus.COMPLETED : OcrStatus.PENDING)
                .ocrProcessedAt(hasOcr ? LocalDateTime.now() : null)
                .build();

        VisitingCard saved = visitingCardRepository.save(card);
        log.info("Created visiting card record: {} by {}", saved.getRecordId(), saved.getUploaderEmail());
        return DocumentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocuments(
            String currentUserEmail,
            String currentUserRole,
            String query,
            RecordStatus status,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<VisitingCard> records;
        if (query != null && !query.trim().isEmpty()) {
            records = visitingCardRepository.searchRecords(query.trim(), pageable);
        } else if (status != null) {
            records = visitingCardRepository.findByStatus(status, pageable);
        } else {
            records = visitingCardRepository.findAll(pageable);
        }

        boolean isSupervisor = "SUPERVISOR".equalsIgnoreCase(currentUserRole) || "ADMIN".equalsIgnoreCase(currentUserRole);
        return records.map(card -> {
            if (!isSupervisor && currentUserEmail != null && !currentUserEmail.equalsIgnoreCase(card.getUploaderEmail())) {
                return null;
            }
            return DocumentResponse.fromEntity(card);
        });
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id, String currentUserEmail, String currentUserRole) {
        VisitingCard card = visitingCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "id", id));

        validateOwnership(card, currentUserEmail, currentUserRole);
        return DocumentResponse.fromEntity(card);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentByRecordId(String recordId, String currentUserEmail, String currentUserRole) {
        VisitingCard card = visitingCardRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("VisitingCard", "recordId", recordId));

        validateOwnership(card, currentUserEmail, currentUserRole);
        return DocumentResponse.fromEntity(card);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long total = visitingCardRepository.count();
        long uploaded = visitingCardRepository.findAll().stream()
                .filter(r -> r.getStatus() == RecordStatus.UPLOADED || r.getStatus() == RecordStatus.VERIFIED)
                .count();
        long pending = visitingCardRepository.findAll().stream()
                .filter(r -> r.getStatus() == RecordStatus.PENDING_UPLOAD || r.getStatus() == RecordStatus.UPLOADING)
                .count();
        long failed = visitingCardRepository.findAll().stream()
                .filter(r -> r.getStatus() == RecordStatus.FAILED)
                .count();
        long ocrCompleted = visitingCardRepository.findByOcrStatus(OcrStatus.COMPLETED).size();

        return DashboardStatsResponse.builder()
                .totalRecords(total)
                .uploadedCount(uploaded)
                .pendingCount(pending)
                .failedCount(failed)
                .ocrCompletedCount(ocrCompleted)
                .build();
    }

    private void validateOwnership(VisitingCard card, String currentUserEmail, String currentUserRole) {
        boolean isSupervisor = "SUPERVISOR".equalsIgnoreCase(currentUserRole) || "ADMIN".equalsIgnoreCase(currentUserRole);
        if (!isSupervisor && currentUserEmail != null && !currentUserEmail.equalsIgnoreCase(card.getUploaderEmail())) {
            throw new ApiException("Access Denied: You do not have permission to view this document.", HttpStatus.FORBIDDEN);
        }
    }
}
