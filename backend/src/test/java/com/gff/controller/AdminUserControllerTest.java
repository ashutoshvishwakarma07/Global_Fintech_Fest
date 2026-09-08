package com.gff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gff.dto.request.CreateUserRequest;
import com.gff.dto.request.UpdateUserRequest;
import com.gff.dto.response.UserDetailResponse;
import com.gff.entity.enums.UserRole;
import com.gff.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
    }

    @Test
    void createUser_Endpoint_Success() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("operator@demo.com");
        req.setPassword("Password123!");
        req.setRole(UserRole.OPERATIONS);
        req.setName("Operator User");

        UserDetailResponse res = new UserDetailResponse();
        res.setId(50L);
        res.setEmail("operator@demo.com");
        res.setRole(UserRole.OPERATIONS);
        res.setName("Operator User");
        res.setActive(true);

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(res);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("operator@demo.com"))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS"));
    }

    @Test
    void getUsers_Endpoint_Success() throws Exception {
        UserDetailResponse res = new UserDetailResponse();
        res.setId(1L);
        res.setEmail("admin@demo.com");
        res.setRole(UserRole.ADMIN);

        when(userService.getUsers(any(), any(), any(), eq(0), eq(50)))
                .thenReturn(new PageImpl<>(List.of(res), org.springframework.data.domain.PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("admin@demo.com"));
    }

    @Test
    void getUserById_Endpoint_Success() throws Exception {
        UserDetailResponse res = new UserDetailResponse();
        res.setId(5L);
        res.setEmail("user5@demo.com");
        res.setRole(UserRole.FIELD_USER);

        when(userService.getUserById(5L)).thenReturn(res);

        mockMvc.perform(get("/admin/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void updateUser_Endpoint_Success() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(UserRole.SUPERVISOR);

        UserDetailResponse res = new UserDetailResponse();
        res.setId(5L);
        res.setRole(UserRole.SUPERVISOR);

        when(userService.updateUser(eq(5L), any(UpdateUserRequest.class))).thenReturn(res);

        mockMvc.perform(put("/admin/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("SUPERVISOR"));
    }

    @Test
    void deactivateUser_Endpoint_Success() throws Exception {
        UserDetailResponse res = new UserDetailResponse();
        res.setId(5L);
        res.setActive(false);

        when(userService.deactivateUser(5L)).thenReturn(res);

        mockMvc.perform(delete("/admin/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
