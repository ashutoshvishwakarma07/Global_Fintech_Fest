package com.gff.dto.response;

import com.gff.entity.enums.UserRole;

/**
 * Safe user profile information for /auth/me and session verification.
 */
public class UserProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String mobile;
    private UserRole role;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String email, String name, String mobile, UserRole role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.mobile = mobile;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
