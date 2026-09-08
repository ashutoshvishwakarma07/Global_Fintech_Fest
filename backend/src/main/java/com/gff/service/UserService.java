package com.gff.service;

import com.gff.dto.request.CreateUserRequest;
import com.gff.dto.request.UpdateUserRequest;
import com.gff.dto.response.UserDetailResponse;
import com.gff.entity.User;
import com.gff.entity.enums.UserRole;
import com.gff.exception.ApiException;
import com.gff.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty()) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApiException("A user with email '" + email + "' already exists", HttpStatus.CONFLICT);
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new ApiException("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
        }

        if (request.getRole() == null) {
            throw new ApiException("Role is required", HttpStatus.BAD_REQUEST);
        }

        // Derive friendly display name if not provided
        String name = request.getName() != null && !request.getName().trim().isEmpty()
                ? request.getName().trim()
                : deriveNameFromEmail(email);

        String passwordHash = passwordEncoder.encode(request.getPassword().trim());

        User user = User.builder()
                .email(email)
                .name(name)
                .passwordHash(passwordHash)
                .role(request.getRole())
                .mobile(request.getMobile() != null ? request.getMobile().trim() : null)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created new user: {} with role [{}]", saved.getEmail(), saved.getRole());

        return UserDetailResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserDetailResponse> getUsers(String search, UserRole role, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<User> usersPage = userRepository.searchUsers(cleanSearch, role, active, pageable);
        return usersPage.map(UserDetailResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found with id: " + id, HttpStatus.NOT_FOUND));
        return UserDetailResponse.fromEntity(user);
    }

    @Transactional
    public UserDetailResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found with id: " + id, HttpStatus.NOT_FOUND));

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getMobile() != null) {
            user.setMobile(request.getMobile().trim());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getPassword().trim().length() < 6) {
                throw new ApiException("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
            }
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
            log.info("Password updated for user: {}", user.getEmail());
        }

        User updated = userRepository.save(user);
        log.info("Updated user: {} (role: {}, active: {})", updated.getEmail(), updated.getRole(), updated.getActive());

        return UserDetailResponse.fromEntity(updated);
    }

    @Transactional
    public UserDetailResponse deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found with id: " + id, HttpStatus.NOT_FOUND));

        user.setActive(false);
        User saved = userRepository.save(user);
        log.info("Deactivated user: {}", saved.getEmail());

        return UserDetailResponse.fromEntity(saved);
    }

    private String deriveNameFromEmail(String email) {
        try {
            String prefix = email.split("@")[0];
            String[] parts = prefix.split("[._-]");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) {
                    sb.append(Character.toUpperCase(p.charAt(0)))
                      .append(p.substring(1).toLowerCase())
                      .append(" ");
                }
            }
            String derived = sb.toString().trim();
            return derived.isEmpty() ? "User" : derived;
        } catch (Exception e) {
            return "User";
        }
    }
}
