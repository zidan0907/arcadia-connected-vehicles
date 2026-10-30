package com.arcadia.common.error;

import com.arcadia.common.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String txId = resolveTransactionId(request);
        log.warn("Domain exception [txId={}, code={}]: {}", txId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), txId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String txId = resolveTransactionId(request);
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ErrorResponse.ofValidation(
                        ErrorCode.INVALID_REQUEST,
                        "The request payload failed validation.",
                        txId,
                        fieldErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String txId = resolveTransactionId(request);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, ex.getMessage(), txId));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadablePayload(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String txId = resolveTransactionId(request);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "Malformed JSON request payload.", txId));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        String txId = resolveTransactionId(request);
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, ex.getMessage(), txId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        String txId = resolveTransactionId(request);
        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), txId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String txId = resolveTransactionId(request);
        log.error("Unhandled server exception [txId={}, uri={}]", txId, request.getRequestURI(), ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                        txId
                ));
    }

    private String resolveTransactionId(HttpServletRequest request) {
        String headerId = request.getHeader(REQUEST_ID_HEADER);
        return (headerId != null && !headerId.isBlank()) ? headerId : UUID.randomUUID().toString();
    }
}