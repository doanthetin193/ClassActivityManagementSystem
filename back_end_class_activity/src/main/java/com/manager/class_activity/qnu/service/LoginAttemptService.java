package com.manager.class_activity.qnu.service;

import com.manager.class_activity.qnu.config.LoginAttemptProperties;
import com.manager.class_activity.qnu.exception.LoginTemporarilyLockedException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginAttemptService {
    LoginAttemptProperties properties;

    Map<String, Integer> failedCounters = new ConcurrentHashMap<>();
    Map<String, Long> lockUntilEpochSeconds = new ConcurrentHashMap<>();
    Map<String, Long> lastFailedEpochSeconds = new ConcurrentHashMap<>();

    public void ensureNotLocked(String username) {
        if (!properties.isEnabled()) {
            return;
        }

        String key = normalizeKey(username);
        long now = Instant.now().getEpochSecond();
        Long lockUntil = lockUntilEpochSeconds.get(key);
        if (lockUntil != null && lockUntil > now) {
            throw new LoginTemporarilyLockedException(lockUntil - now);
        }

        if (lockUntil != null && lockUntil <= now) {
            clear(key);
        }
    }

    public void onLoginSuccess(String username) {
        if (!properties.isEnabled()) {
            return;
        }
        clear(normalizeKey(username));
    }

    public int onLoginFailure(String username) {
        if (!properties.isEnabled()) {
            return -1;
        }

        String key = normalizeKey(username);
        long now = Instant.now().getEpochSecond();
        int resetWindow = Math.max(1, properties.getResetWindowSeconds());

        Long lastFailed = lastFailedEpochSeconds.get(key);
        if (lastFailed != null && now - lastFailed > resetWindow) {
            failedCounters.put(key, 0);
        }

        int current = failedCounters.getOrDefault(key, 0) + 1;
        failedCounters.put(key, current);
        lastFailedEpochSeconds.put(key, now);

        if (current >= Math.max(1, properties.getMaxFailures())) {
            long lockSeconds = Math.max(1, properties.getLockSeconds());
            lockUntilEpochSeconds.put(key, now + lockSeconds);
            throw new LoginTemporarilyLockedException(lockSeconds);
        }

        return Math.max(0, properties.getMaxFailures() - current);
    }

    public void resetAll() {
        failedCounters.clear();
        lockUntilEpochSeconds.clear();
        lastFailedEpochSeconds.clear();
    }

    public int lockSize() {
        return lockUntilEpochSeconds.size();
    }

    private void clear(String key) {
        failedCounters.remove(key);
        lockUntilEpochSeconds.remove(key);
        lastFailedEpochSeconds.remove(key);
    }

    private String normalizeKey(String username) {
        if (username == null || username.isBlank()) {
            return "__unknown__";
        }
        return username.trim().toLowerCase();
    }
}
