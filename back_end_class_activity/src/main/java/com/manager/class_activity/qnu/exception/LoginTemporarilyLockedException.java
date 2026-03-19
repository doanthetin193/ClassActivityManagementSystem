package com.manager.class_activity.qnu.exception;

import lombok.Getter;

@Getter
public class LoginTemporarilyLockedException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginTemporarilyLockedException(long retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_REQUESTS.getMessage());
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
