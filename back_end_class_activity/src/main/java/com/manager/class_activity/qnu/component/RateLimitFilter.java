package com.manager.class_activity.qnu.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.class_activity.qnu.config.RateLimitProperties;
import com.manager.class_activity.qnu.dto.response.JsonResponse;
import com.manager.class_activity.qnu.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitFilter extends OncePerRequestFilter {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();
    RateLimitProperties rateLimitProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !rateLimitProperties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String clientIp = extractClientIp(request);
        String method = request.getMethod();

        if (rateLimitProperties.getTrustedIps().contains(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitDecision decision = new RateLimitDecision(false, 0);
        if (isTargetRequest(method, uri, rateLimitProperties.getLogin())) {
            decision = isRateLimited(
                    "login:" + clientIp,
                    rateLimitProperties.getLogin().getMaxRequests(),
                    rateLimitProperties.getLogin().getWindowSeconds()
            );
        } else if (isTargetRequest(method, uri, rateLimitProperties.getRollCall())) {
            String subjectKey = resolveSubjectKey(clientIp);
            decision = isRateLimited(
                "rollcall:" + subjectKey,
                    rateLimitProperties.getRollCall().getMaxRequests(),
                    rateLimitProperties.getRollCall().getWindowSeconds()
            );
        }

        if (decision.blocked()) {
            log.warn("Rate limit exceeded. ip={}, uri={}", clientIp, uri);
            writeRateLimitResponse(response, decision.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTargetRequest(String method, String uri, RateLimitProperties.EndpointRule rule) {
        if (!rule.isEnabled()) {
            return false;
        }
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return uri.endsWith(rule.getPath());
    }

    private RateLimitDecision isRateLimited(String bucketKey, int maxRequests, int windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            return new RateLimitDecision(false, 0);
        }

        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        cleanupExpiredBuckets(now, windowMs, rateLimitProperties.getCleanupThreshold());

        Deque<Long> timestamps = requestBuckets.computeIfAbsent(bucketKey, key -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                long oldest = timestamps.peekFirst() == null ? now : timestamps.peekFirst();
                long retryAfterMs = Math.max(0, (oldest + windowMs) - now);
                long retryAfterSeconds = (retryAfterMs + 999) / 1000;
                return new RateLimitDecision(true, retryAfterSeconds);
            }

            timestamps.addLast(now);
            return new RateLimitDecision(false, 0);
        }
    }

    private void cleanupExpiredBuckets(long now, long windowMs, int cleanupThreshold) {
        if (requestBuckets.size() < cleanupThreshold) {
            return;
        }

        requestBuckets.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
                    timestamps.pollFirst();
                }
                return timestamps.isEmpty();
            }
        });
    }

    private void writeRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }
        response.getWriter().write(objectMapper.writeValueAsString(JsonResponse.error(errorCode)));
        response.flushBuffer();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveSubjectKey(String clientIp) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            if (username != null && !username.isBlank() && !"anonymousUser".equalsIgnoreCase(username)) {
                return "user:" + username;
            }
        }
        return "ip:" + clientIp;
    }

    private record RateLimitDecision(boolean blocked, long retryAfterSeconds) {
    }

    public void resetAllBuckets() {
        requestBuckets.clear();
        log.info("Rate limit buckets have been reset");
    }

    public int bucketSize() {
        return requestBuckets.size();
    }
}