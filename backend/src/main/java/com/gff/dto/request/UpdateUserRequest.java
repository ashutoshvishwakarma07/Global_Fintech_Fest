package com.gff.dto.request;

import com.gff.entity.enums.UserRole;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    private UserRole role;

    private Boolean active;

    private String name;

    private String mobile;

    @Size(min = 6, message = "Password must be at least 6 characters if provided")
    private String password;

    public UpdateUserRequest() {
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
