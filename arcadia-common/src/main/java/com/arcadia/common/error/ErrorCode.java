package com.arcadia.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request payload failed validation."),
    INVALID_TELEMETRY(HttpStatus.BAD_REQUEST, "Trip telemetry contains invalid or missing fields."),
    INVALID_COMMAND(HttpStatus.BAD_REQUEST, "The requested vehicle command or parameter is invalid."),

    // 401 Unauthorized / 403 Forbidden
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username/email or password."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "The authentication token has expired."),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "The authentication token has been revoked."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "The provided token is malformed or invalid."),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED, "User account is disabled."),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "User account is locked."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested user account was not found."),
    DRIVER_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested driver was not found."),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested vehicle VIN was not found."),
    COMMAND_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested command ID was not found."),
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested maintenance alert was not found."),

    // 409 Conflict
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "The resource state conflicts with the current request."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "A resource with the same identifier already exists."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Username or email is already registered."),
    COMMAND_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "A conflicting command is already in progress for this vehicle."),

    // 408 / 504 Timeout
    VEHICLE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Vehicle did not acknowledge command within the configured timeout window."),

    // 502 / 503 External / Downstream Failure
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "An upstream or downstream external service failed to respond."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while processing the request.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}