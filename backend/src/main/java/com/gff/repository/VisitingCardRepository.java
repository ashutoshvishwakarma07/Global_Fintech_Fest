package com.gff.repository;

import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import com.gff.entity.enums.RecordStatus;
import com.gff.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitingCardRepository extends JpaRepository<VisitingCard, Long> {

    /**
     * Find by unique client-generated record ID (e.g., REC-...).
     */
    Optional<VisitingCard> findByRecordId(String recordId);

    boolean existsByRecordId(String recordId);

    /**
     * Filter by record lifecycle status.
     */
    Page<VisitingCard> findByStatus(RecordStatus status, Pageable pageable);

    /**
     * Filter by uploader role.
     */
    Page<VisitingCard> findByUploaderRole(UserRole role, Pageable pageable);

    /**
     * Filter pending/processing OCR items for scheduled batch processing.
     */
    List<VisitingCard> findByOcrStatus(OcrStatus ocrStatus);

    List<VisitingCard> findByOcrStatusIn(List<OcrStatus> statuses);

    /**
     * Query all visiting cards created between given timestamps (e.g. today's uploads).
     */
    List<VisitingCard> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * Search query matching frontend search bar (by record ID, uploader name, or email).
     */
    @Query("SELECT v FROM VisitingCard v WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(v.recordId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(v.uploaderName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(v.uploaderEmail) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<VisitingCard> searchRecords(@Param("query") String query, Pageable pageable);
}
