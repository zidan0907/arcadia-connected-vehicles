package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

import java.util.Objects;

public abstract class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    protected ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
