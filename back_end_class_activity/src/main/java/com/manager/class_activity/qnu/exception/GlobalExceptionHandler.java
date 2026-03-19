package com.manager.class_activity.qnu.exception;

import com.manager.class_activity.qnu.dto.response.JsonResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(value = Exception.class)
//    ResponseEntity<JsonResponse<String>> handleRuntimeException(Exception e) {
//        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
//        return ResponseEntity.status(errorCode.getStatusCode()).body(JsonResponse.error(errorCode));
//    }

    @ExceptionHandler(value = BadException.class)
    ResponseEntity<JsonResponse<String>> handleAppException(BadException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatusCode()).body(JsonResponse.error(errorCode));
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<JsonResponse<String>> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return ResponseEntity.status(errorCode.getStatusCode()).body(JsonResponse.error(errorCode));
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    ResponseEntity<JsonResponse<String>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(errorCode.getStatusCode()).body(JsonResponse.error(errorCode));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<JsonResponse<String>> handleArgumentNotValidException(MethodArgumentNotValidException e) {
        String enumKey = ObjectUtils.isNotEmpty(e.getBindingResult().getFieldError())
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorConstant.UNCATEGORIZED_EXCEPTION;
        ErrorCode errorCode = ErrorCode.getError(enumKey);
        return ResponseEntity.status(errorCode.getStatusCode()).body(JsonResponse.error(errorCode));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleInvalidTokenException(InvalidTokenException ex) {
        return ResponseEntity.status(ErrorCode.TOKEN_INVALID.getStatusCode()).body(JsonResponse.error(ErrorCode.TOKEN_INVALID));
    }

    @ExceptionHandler(LoginTemporarilyLockedException.class)
    ResponseEntity<JsonResponse<String>> handleLoginTemporarilyLockedException(LoginTemporarilyLockedException e) {
        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        return ResponseEntity.status(errorCode.getStatusCode()).headers(headers).body(JsonResponse.error(errorCode));
    }

    @ExceptionHandler(LoginFailedAttemptException.class)
    ResponseEntity<JsonResponse<String>> handleLoginFailedAttemptException(LoginFailedAttemptException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Login-Attempts-Remaining", String.valueOf(e.getRemainingAttempts()));
        return ResponseEntity.status(errorCode.getStatusCode()).headers(headers).body(JsonResponse.error(errorCode));
    }

}
