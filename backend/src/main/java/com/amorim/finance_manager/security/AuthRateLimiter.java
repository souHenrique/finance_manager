package com.amorim.finance_manager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthRateLimiter {

    private final AuthRateLimitProperties properties;
    private final Clock financeClock;
    private final Map<String, AttemptWindow> windows = new LinkedHashMap<>();

    public synchronized RateLimitResult tryAcquire(String path, String clientIp) {
        AuthRateLimitProperties.Rule rule = ruleFor(path);
        String key = path + ':' + clientIp;
        long now = financeClock.millis();

        removeExpiredWindows(now);

        AttemptWindow currentWindow = windows.get(key);

        if (currentWindow == null || currentWindow.isExpired(now, rule.window())) {
            ensureCapacity();
            windows.put(key, new AttemptWindow(now, 1));
            return RateLimitResult.permitted();
        }

        if (currentWindow.attempts() >= rule.maxAttempts()) {
            return RateLimitResult.rejected(currentWindow.retryAfterSeconds(now, rule.window()));
        }

        windows.put(key, currentWindow.increment());
        return RateLimitResult.permitted();
    }

    public synchronized void clear() {
        windows.clear();
    }

    private AuthRateLimitProperties.Rule ruleFor(String path) {
        return "/api/v1/auth/login".equals(path)
                ? properties.login()
                : properties.register();
    }

    private void removeExpiredWindows(long now) {
        Iterator<Map.Entry<String, AttemptWindow>> iterator = windows.entrySet().iterator();
        Duration longestWindow = properties.login().window().compareTo(properties.register().window()) >= 0
                ? properties.login().window()
                : properties.register().window();

        while (iterator.hasNext()) {
            AttemptWindow attemptWindow = iterator.next().getValue();

            if (attemptWindow.isExpired(now, longestWindow)) {
                iterator.remove();
            }
        }
    }

    private void ensureCapacity() {
        if (windows.size() < properties.maxTrackedClients()) {
            return;
        }

        Iterator<String> keys = windows.keySet().iterator();

        if (keys.hasNext()) {
            keys.next();
            keys.remove();
        }
    }

    private record AttemptWindow(long startedAt, int attempts) {

        boolean isExpired(long now, Duration window) {
            return now - startedAt >= window.toMillis();
        }

        AttemptWindow increment() {
            return new AttemptWindow(startedAt, attempts + 1);
        }

        long retryAfterSeconds(long now, Duration window) {
            long remainingMillis = Math.max(1, window.toMillis() - (now - startedAt));
            return Math.max(1, (long) Math.ceil(remainingMillis / 1000.0));
        }
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

        static RateLimitResult permitted() {
            return new RateLimitResult(true, 0);
        }

        static RateLimitResult rejected(long retryAfterSeconds) {
            return new RateLimitResult(false, retryAfterSeconds);
        }
    }
}
