package com.gff.controller;

import com.gff.dto.request.LoginRequest;
import com.gff.dto.response.ApiResponse;
import com.gff.dto.response.AuthResponse;
import com.gff.dto.response.UserProfileResponse;
import com.gff.entity.User;
import com.gff.exception.ApiException;
import com.gff.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        // Set secure HttpOnly cookie for session management
        ResponseCookie cookie = ResponseCookie.from("auth_token", authResponse.getToken())
                .httpOnly(true)
                .secure(false) // Set to true when on HTTPS in production
                .sameSite("Lax")
                .path("/")
                .maxAge(86400) // 24 hours
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        String email = (String) request.getAttribute("currentUserEmail");

        if (currentUser == null && email == null) {
            throw new ApiException("Full authentication is required to access this resource", HttpStatus.UNAUTHORIZED);
        }

        UserProfileResponse profile;
        if (currentUser != null) {
            profile = new UserProfileResponse(
                    currentUser.getId(),
                    currentUser.getEmail(),
                    currentUser.getName(),
                    currentUser.getMobile(),
                    currentUser.getRole()
            );
        } else {
            profile = authService.getMe(email);
        }

        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", profile));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        // Clear HttpOnly auth cookie
        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0) // Expire immediately
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
