package com.gff.service;

import com.gff.dto.request.CreateUserRequest;
import com.gff.dto.request.UpdateUserRequest;
import com.gff.dto.response.UserDetailResponse;
import com.gff.entity.User;
import com.gff.entity.enums.UserRole;
import com.gff.exception.ApiException;
import com.gff.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUser_Success() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("newuser@qualtech.com");
        req.setPassword("Secret123!");
        req.setRole(UserRole.OPERATIONS);
        req.setName("New Operator");

        when(userRepository.existsByEmail("newuser@qualtech.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed_Secret123!");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });

        UserDetailResponse res = userService.createUser(req);

        assertNotNull(res);
        assertEquals(101L, res.getId());
        assertEquals("newuser@qualtech.com", res.getEmail());
        assertEquals(UserRole.OPERATIONS, res.getRole());
        assertEquals("New Operator", res.getName());
        assertTrue(res.getActive());
        verify(passwordEncoder).encode("Secret123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsConflict() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("existing@qualtech.com");
        req.setPassword("Secret123!");
        req.setRole(UserRole.FIELD_USER);

        when(userRepository.existsByEmail("existing@qualtech.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> userService.createUser(req));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_ShortPassword_ThrowsBadRequest() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("test@qualtech.com");
        req.setPassword("123");
        req.setRole(UserRole.FIELD_USER);

        when(userRepository.existsByEmail("test@qualtech.com")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> userService.createUser(req));
        assertTrue(ex.getMessage().contains("at least 6 characters"));
    }

    @Test
    void updateUser_Success() {
        User existing = User.builder()
                .id(10L)
                .email("user@qualtech.com")
                .name("Old Name")
                .role(UserRole.FIELD_USER)
                .active(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setRole(UserRole.SUPERVISOR);
        updateReq.setName("Updated Name");
        updateReq.setActive(false);

        UserDetailResponse res = userService.updateUser(10L, updateReq);

        assertNotNull(res);
        assertEquals(UserRole.SUPERVISOR, res.getRole());
        assertEquals("Updated Name", res.getName());
        assertFalse(res.getActive());
    }

    @Test
    void deactivateUser_Success() {
        User existing = User.builder()
                .id(20L)
                .email("active@qualtech.com")
                .active(true)
                .build();

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDetailResponse res = userService.deactivateUser(20L);

        assertNotNull(res);
        assertFalse(res.getActive());
    }
}
