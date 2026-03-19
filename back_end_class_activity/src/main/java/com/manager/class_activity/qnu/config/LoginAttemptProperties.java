package com.manager.class_activity.qnu.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "login-attempt")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginAttemptProperties {
    boolean enabled = true;
    int maxFailures = 5;
    int lockSeconds = 900;
    int resetWindowSeconds = 900;
}
