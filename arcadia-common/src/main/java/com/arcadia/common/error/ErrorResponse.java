package com.arcadia.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String errorCode, String message, Instant timestamp, Map<String, String> details) {

    public ErrorResponse(String errorCode, String message, Instant timestamp) {
        this(errorCode, message, timestamp, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, Instant.now());
    }

    public static ErrorResponse validation(String message, Map<String, String> details) {
        return new ErrorResponse(ErrorCode.VALIDATION_FAILED.getCode(), message, Instant.now(), details);
    }
}
