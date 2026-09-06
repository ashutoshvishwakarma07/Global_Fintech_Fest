package com.gff.entity;

import com.gff.entity.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Application user entity (Field User / Supervisor / Admin).
 */
@Entity
@Table(name = "app_users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(length = 32)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String email;
        private String passwordHash;
        private String name;
        private String mobile;
        private UserRole role;
        private String avatarUrl;
        private Boolean active = true;

        public UserBuilder id(Long id) { this.id = id; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public UserBuilder name(String name) { this.name = name; return this; }
        public UserBuilder mobile(String mobile) { this.mobile = mobile; return this; }
        public UserBuilder role(UserRole role) { this.role = role; return this; }
        public UserBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public UserBuilder active(Boolean active) { this.active = active; return this; }

        public User build() {
            User u = new User();
            u.id = this.id;
            u.email = this.email;
            u.passwordHash = this.passwordHash;
            u.name = this.name;
            u.mobile = this.mobile;
            u.role = this.role;
            u.avatarUrl = this.avatarUrl;
            u.active = this.active;
            return u;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
