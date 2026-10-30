package com.arcadia.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String transactionId,
        String errorCode,
        String message,
        Instant timestamp,
        Map<String, String> details
) {
    public static ErrorResponse of(ErrorCode code, String customMessage, String transactionId) {
        return ErrorResponse.builder()
                .transactionId(transactionId != null && !transactionId.isBlank() ? transactionId : UUID.randomUUID().toString())
                .errorCode(code.name())
                .message(customMessage != null && !customMessage.isBlank() ? customMessage : code.getDefaultMessage())
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse ofValidation(
            ErrorCode code,
            String customMessage,
            String transactionId,
            Map<String, String> fieldErrors
    ) {
        return ErrorResponse.builder()
                .transactionId(transactionId != null && !transactionId.isBlank() ? transactionId : UUID.randomUUID().toString())
                .errorCode(code.name())
                .message(customMessage != null && !customMessage.isBlank() ? customMessage : code.getDefaultMessage())
                .timestamp(Instant.now())
                .details(fieldErrors)
                .build();
    }
}