package com.manager.class_activity.qnu.exception;

import lombok.Getter;

@Getter
public class LoginFailedAttemptException extends RuntimeException {
    private final int remainingAttempts;

    public LoginFailedAttemptException(int remainingAttempts) {
        super(ErrorCode.UNAUTHENTICATED.getMessage());
        this.remainingAttempts = remainingAttempts;
    }
}
