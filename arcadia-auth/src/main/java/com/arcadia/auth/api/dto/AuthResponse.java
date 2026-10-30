package com.arcadia.auth.api.dto;

import com.arcadia.auth.api.domain.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Stateless JWT authentication response containing access/refresh tokens and user profile")
public record AuthResponse(

        @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Signed JWT refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "Authorization header scheme", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token validity duration in seconds", example = "900")
        long expiresIn,

        @Schema(description = "Unique Arcadia user/driver ID", example = "user-1042")
        String userId,

        @Schema(description = "Authenticated username", example = "driver.1042")
        String username,

        @Schema(description = "User email address", example = "driver1042@arcadia.com")
        String email,

        @Schema(description = "Full display name", example = "Rohan Verma")
        String fullName,

        @Schema(description = "Granted platform role", example = "ROLE_DRIVER")
        Role role,

        @Schema(description = "Primary vehicle VIN linked to this driver", example = "MAKZ1234567890")
        String assignedVin,

        @Schema(description = "UTC timestamp when the token was issued", example = "2026-10-28T15:35:00Z")
        Instant issuedAt
) {
}