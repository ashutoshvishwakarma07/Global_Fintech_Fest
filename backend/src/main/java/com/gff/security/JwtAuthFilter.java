package com.gff.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gff.config.JwtUtil;
import com.gff.entity.User;
import com.gff.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Filter to validate JWT token from Bearer Authorization header or HttpOnly 'auth_token' cookie.
 * Attaches authenticated user to request attributes and protects secure endpoints.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract token from header or cookie
        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            Map<String, Object> claims = jwtUtil.validateAndExtractClaims(token);
            if (claims != null && claims.containsKey("sub")) {
                String email = (String) claims.get("sub");
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (Boolean.TRUE.equals(user.getActive())) {
                        request.setAttribute("currentUser", user);
                        request.setAttribute("currentUserEmail", user.getEmail());
                        request.setAttribute("currentUserRole", user.getRole().name());
                        request.setAttribute("currentUserId", user.getId());
                    } else {
                        log.warn("Blocked request for inactive user: {}", email);
                    }
                }
            }
        }

        // 2. Check if endpoint requires authentication
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Always allow CORS preflights, health checks, login, and static error paths
        if ("OPTIONS".equalsIgnoreCase(method)
                || uri.endsWith("/health")
                || uri.endsWith("/auth/login")
                || uri.contains("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Protected endpoints: /documents/**, /auth/me, /auth/logout
        if (uri.contains("/documents") || uri.endsWith("/auth/me") || uri.endsWith("/auth/logout")) {
            if (request.getAttribute("currentUser") == null) {
                sendUnauthorizedError(request, response, "Authentication required to access this resource");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Priority 1: Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // Priority 2: HttpOnly cookie 'auth_token'
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void sendUnauthorizedError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }

    @Configuration
    public static class FilterConfig {
        @Bean
        public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtUtil jwtUtil, UserRepository userRepository) {
            FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new JwtAuthFilter(jwtUtil, userRepository));
            // Run after CorsFilter (HIGHEST_PRECEDENCE) so CORS headers are added to 401 responses
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            registration.addUrlPatterns("/*");
            return registration;
        }
    }
}
