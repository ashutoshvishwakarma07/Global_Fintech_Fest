package com.gff.controller;

import com.gff.dto.request.ShareCardRequest;
import com.gff.dto.response.ApiResponse;
import com.gff.dto.response.DocumentResponse;
import com.gff.entity.User;
import com.gff.exception.ApiException;
import com.gff.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller providing REST endpoints for visiting card operations.
 */
@RestController
@RequestMapping("/visiting-cards")
public class VisitingCardController {

    private final DocumentService documentService;

    public VisitingCardController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Share visiting card details with a lead recipient via email.
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<DocumentResponse>> shareVisitingCard(
            @PathVariable String id,
            @Valid @RequestBody ShareCardRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = (User) httpRequest.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ApiException("Full authentication is required to share visiting cards", HttpStatus.UNAUTHORIZED);
        }

        DocumentResponse record = documentService.shareVisitingCard(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Visiting card details shared successfully with lead", record));
    }
}
