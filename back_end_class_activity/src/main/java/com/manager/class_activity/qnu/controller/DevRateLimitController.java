package com.manager.class_activity.qnu.controller;

import com.manager.class_activity.qnu.component.RateLimitFilter;
import com.manager.class_activity.qnu.dto.response.JsonResponse;
import com.manager.class_activity.qnu.service.LoginAttemptService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/dev/rate-limit")
public class DevRateLimitController {
    RateLimitFilter rateLimitFilter;
    LoginAttemptService loginAttemptService;

    @PostMapping("/reset")
    public JsonResponse<String> resetRateLimit() {
        int bucketBefore = rateLimitFilter.bucketSize();
        int lockBefore = loginAttemptService.lockSize();
        rateLimitFilter.resetAllBuckets();
        loginAttemptService.resetAll();
        return JsonResponse.success("Rate limit reset success. Cleared buckets: " + bucketBefore + ", login locks: " + lockBefore);
    }
}