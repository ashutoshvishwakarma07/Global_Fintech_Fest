package com.gff.controller;

import com.gff.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic and health endpoint to verify deployment timestamp,
 * active environment profile, and version details.
 */
@RestController
@RequestMapping("/system")
public class SystemInfoController {

    private static final Instant SERVER_START_TIME = Instant.now();

    @Value("${spring.application.name:gff-backend}")
    private String applicationName;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemInfo() {
        DateTimeFormatter istFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.of("Asia/Kolkata"));

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", applicationName);
        info.put("version", "1.1.0");
        info.put("activeProfile", activeProfile);
        info.put("serverStartTimeUtc", SERVER_START_TIME.toString());
        info.put("serverStartTimeIst", istFormatter.format(SERVER_START_TIME));
        info.put("currentServerTimeIst", istFormatter.format(Instant.now()));
        info.put("status", "UP");

        return ResponseEntity.ok(ApiResponse.success("System deployment info retrieved", info));
    }
}
