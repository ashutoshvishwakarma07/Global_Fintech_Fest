package com.gff.service;

import com.gff.config.JwtUtil;
import com.gff.dto.request.LoginRequest;
import com.gff.dto.response.AuthResponse;
import com.gff.dto.response.UserProfileResponse;
import com.gff.entity.User;
import com.gff.entity.enums.UserRole;
import com.gff.exception.ApiException;
import com.gff.repository.UserRepository;
import com.gff.security.LoginRateLimiter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;
    private final JdbcTemplate jdbcTemplate;

    public AuthService(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            LoginRateLimiter rateLimiter,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    @Transactional
    public void seedInitialUsers() {
        try {
            jdbcTemplate.execute("ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check");
        } catch (Exception e) {
            log.warn("Notice updating role constraint: {}", e.getMessage());
        }

        // Seed and migrate initial 3 users with secure BCrypt-hashed passwords
        seedOrMigrateUser("admin@demo.com", "Admin@123", "Admin User", "9900112233", UserRole.ADMIN);
        seedOrMigrateUser("user1@demo.com", "Demo@123", "Rahul Sharma", "9876543210", UserRole.FIELD_USER);
        seedOrMigrateUser("user2@demo.com", "Demo@123", "Priya Verma", "9812345678", UserRole.SUPERVISOR);

        // Also ensure example accounts have BCrypt hashes if present
        seedOrMigrateUser("user1@example.com", "Demo@123", "Field User One", "9876543210", UserRole.FIELD_USER);
        seedOrMigrateUser("user2@example.com", "Demo@123", "Field User Two", "9876543211", UserRole.FIELD_USER);
        seedOrMigrateUser("admin@example.com", "Admin@123", "Lead Supervisor", "9876543212", UserRole.ADMIN);
    }

    private void seedOrMigrateUser(String email, String plainPassword, String name, String mobile, UserRole role) {
        try {
            Optional<User> existingOpt = userRepository.findByEmail(email);
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                // If password hash is missing or outdated, update to BCrypt
                if (existing.getPasswordHash() == null || !existing.getPasswordHash().startsWith("$2a$") && !existing.getPasswordHash().startsWith("$2b$")) {
                    existing.setPasswordHash(passwordEncoder.encode(plainPassword));
                    existing.setRole(role);
                    userRepository.save(existing);
                    log.info("Migrated password hash for existing user: {}", email);
                }
            } else {
                User user = User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(plainPassword))
                        .name(name)
                        .mobile(mobile)
                        .role(role)
                        .active(true)
                        .build();
                userRepository.save(user);
                log.info("Seeded initial user: {} [{}] with BCrypt hash", email, role);
            }
        } catch (Exception e) {
            log.warn("Database user seeding notice for {}: {}", email, e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String rawPassword = request.getPassword() != null ? request.getPassword() : "";

        // 1. Rate limiter check for brute force protection
        rateLimiter.checkAllowed(email);

        // 2. Query database for user
        User user = userRepository.findByEmail(email).orElse(null);

        // 3. Constant-time secure password verification
        boolean passwordValid = false;
        if (user != null && user.getPasswordHash() != null) {
            passwordValid = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        }

        if (user == null || !passwordValid) {
            rateLimiter.recordFailure(email);
            log.warn("Failed authentication attempt for email: {}", email);
            throw new ApiException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        // 4. Verify account active status
        if (!Boolean.TRUE.equals(user.getActive())) {
            log.warn("Authentication blocked for inactive user: {}", email);
            throw new ApiException("User account is inactive. Please contact your administrator.", HttpStatus.FORBIDDEN);
        }

        // 5. Successful login: reset rate limiter failures
        rateLimiter.recordSuccess(email);

        // 6. Generate signed JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );

        log.info("User successfully authenticated: {} [{}]", user.getEmail(), user.getRole());

        // 7. Return safe DTO without password or hash
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .mobile(user.getMobile())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getMobile(),
                user.getRole()
        );
    }
}
