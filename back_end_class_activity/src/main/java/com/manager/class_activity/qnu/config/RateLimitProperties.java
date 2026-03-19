package com.manager.class_activity.qnu.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rate-limit")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RateLimitProperties {
    boolean enabled = true;
    int cleanupThreshold = 2000;
    Set<String> trustedIps = new HashSet<>();
    EndpointRule login = new EndpointRule(true, "/auth/log-in", 5, 60);
    EndpointRule rollCall = new EndpointRule(true, "/attendance/roll-call", 20, 60);

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EndpointRule {
        boolean enabled;
        String path;
        int maxRequests;
        int windowSeconds;

        public EndpointRule() {
        }

        public EndpointRule(boolean enabled, String path, int maxRequests, int windowSeconds) {
            this.enabled = enabled;
            this.path = path;
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}