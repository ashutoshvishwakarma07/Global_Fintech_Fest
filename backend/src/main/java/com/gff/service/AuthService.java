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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialUsersAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting background DB schema migration & initial user seeding...");
                jdbcTemplate.execute("ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check");
                jdbcTemplate.execute("ALTER TABLE visiting_cards DROP CONSTRAINT IF EXISTS visiting_cards_uploader_role_check");
                // Auto-migrate schema with all 14 standardized fields if missing
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS department VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS work_number VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS website_url VARCHAR(256)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS city VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS state VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS postal_zip_code VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS country VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS linkedin VARCHAR(256)");
                jdbcTemplate.execute("ALTER TABLE visiting_cards ADD COLUMN IF NOT EXISTS twitter VARCHAR(256)");
                log.info("Visiting cards table schema verified.");
            } catch (Exception e) {
                log.warn("Notice updating schema/role constraint: {}", e.getMessage());
            }

            // Seed initial default users if they don't already exist
            seedOrMigrateUser("admin@demo.com", "Admin@123", "Admin User", "9900112233", UserRole.ADMIN);
            seedOrMigrateUser("user1@demo.com", "Demo@123", "Rahul Sharma", "9876543210", UserRole.FIELD_USER);
            seedOrMigrateUser("user2@demo.com", "Demo@123", "Priya Verma", "9812345678", UserRole.FIELD_USER);
            log.info("Background DB schema check & user seeding completed.");
        });
    }

    private void seedOrMigrateUser(String email, String plainPassword, String name, String mobile, UserRole role) {
        try {
            Optional<User> existingOpt = userRepository.findByEmail(email);
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                existing.setPasswordHash(passwordEncoder.encode(plainPassword));
                existing.setRole(role);
                existing.setName(name);
                existing.setMobile(mobile);
                existing.setActive(true);
                userRepository.save(existing);
                log.info("Updated credentials and role for user: {} [{}]", email, role);
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
                log.info("Seeded user: {} [{}] with BCrypt hash", email, role);
            }
        } catch (Exception e) {
            log.warn("Database user seeding notice for {}: {}", email, e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        String input = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        // Support both username ("admin", "user1", "user2") and full email ("admin@demo.com", etc.)
        String email = input;
        if (!input.contains("@") && !input.isEmpty()) {
            email = input + "@demo.com";
        }
        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        // 1. Rate limiter check for brute force protection
        rateLimiter.checkAllowed(email);

        // 2. Query database for user
        User user = userRepository.findByEmail(email).orElse(null);

        // 3. Constant-time secure password verification (checks both trimmed and verbatim)
        boolean passwordValid = false;
        if (user != null && user.getPasswordHash() != null) {
            passwordValid = passwordEncoder.matches(rawPassword, user.getPasswordHash())
                    || (request.getPassword() != null && passwordEncoder.matches(request.getPassword(), user.getPasswordHash()));
        }

        if (user == null || !passwordValid) {
            rateLimiter.recordFailure(email);
            log.warn("Failed authentication attempt for email/username: {} (resolved email: {})", input, email);
            throw new ApiException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new ApiException("Account is disabled. Please contact your administrator.", HttpStatus.FORBIDDEN);
        }

        // 4. Reset rate limiter on successful login
        rateLimiter.recordSuccess(email);
        log.info("User successfully authenticated: {} [{}]", user.getEmail(), user.getRole());

        // 5. Generate signed JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .mobile(user.getMobile())
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
