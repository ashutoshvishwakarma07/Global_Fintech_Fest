package com.gff.security;

import com.gff.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: in-memory sliding window rate limiter for login attempts.
 * Limits failed attempts to 5 per 5-minute window; locks out for 15 minutes upon limit breach.
 */
@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300; // 5 minutes
    private static final long LOCKOUT_SECONDS = 900; // 15 minutes

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private static class AttemptInfo {
        int failedCount;
        Instant firstAttempt;
        Instant lockedUntil;

        AttemptInfo(Instant now) {
            this.failedCount = 1;
            this.firstAttempt = now;
            this.lockedUntil = null;
        }
    }

    public void checkAllowed(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        AttemptInfo info = attempts.get(key.toLowerCase().trim());
        if (info == null) {
            return;
        }

        Instant now = Instant.now();
        if (info.lockedUntil != null && now.isBefore(info.lockedUntil)) {
            long remainingSeconds = info.lockedUntil.getEpochSecond() - now.getEpochSecond();
            long minutes = Math.max(1, remainingSeconds / 60);
            log.warn("Rate limit triggered for key [{}]. Account locked for {} more minutes.", key, minutes);
            throw new ApiException("Too many failed login attempts. Please try again in " + minutes + " minute(s).", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        String cleanKey = key.toLowerCase().trim();
        Instant now = Instant.now();

        attempts.compute(cleanKey, (k, info) -> {
            if (info == null) {
                return new AttemptInfo(now);
            }

            // If existing window has expired, reset window
            if (info.firstAttempt.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
                info.failedCount = 1;
                info.firstAttempt = now;
                info.lockedUntil = null;
                return info;
            }

            info.failedCount++;
            if (info.failedCount >= MAX_ATTEMPTS) {
                info.lockedUntil = now.plusSeconds(LOCKOUT_SECONDS);
                log.warn("Brute force threshold reached for key [{}]. Lockout applied until {}", cleanKey, info.lockedUntil);
            }
            return info;
        });
    }

    public void recordSuccess(String key) {
        if (key != null) {
            attempts.remove(key.toLowerCase().trim());
        }
    }
}
