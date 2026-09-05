package com.gff.dto.response;

import com.gff.entity.enums.UserRole;

public class AuthResponse {

    private String token;
    private Long id;
    private String email;
    private String name;
    private String mobile;
    private UserRole role;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, String email, String name, String mobile, UserRole role) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.name = name;
        this.mobile = mobile;
        this.role = role;
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private String token;
        private Long id;
        private String email;
        private String name;
        private String mobile;
        private UserRole role;

        public AuthResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuthResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public AuthResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AuthResponseBuilder mobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        public AuthResponseBuilder role(UserRole role) {
            this.role = role;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, id, email, name, mobile, role);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
