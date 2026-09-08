package com.gff.controller;

import com.gff.dto.request.CreateUserRequest;
import com.gff.dto.request.UpdateUserRequest;
import com.gff.dto.response.ApiResponse;
import com.gff.dto.response.UserDetailResponse;
import com.gff.entity.enums.UserRole;
import com.gff.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create user by Admin.
     * URL: POST /api/v1/admin/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDetailResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDetailResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", created));
    }

    /**
     * Get paginated users with optional search and filters.
     * URL: GET /api/v1/admin/users?search=...&role=...&active=...&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDetailResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<UserDetailResponse> users = userService.getUsers(search, role, active, page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Get user details by ID.
     * URL: GET /api/v1/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable Long id) {
        UserDetailResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved", user));
    }

    /**
     * Update user details / role / active status / password.
     * URL: PUT /api/v1/admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserDetailResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    /**
     * Deactivate user (soft-delete).
     * URL: DELETE /api/v1/admin/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> deactivateUser(@PathVariable Long id) {
        UserDetailResponse deactivated = userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", deactivated));
    }
}
