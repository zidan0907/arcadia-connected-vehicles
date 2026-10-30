package com.arcadia.auth.api.dto;

import com.arcadia.auth.api.domain.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Confirmation payload returned after a user account is created via Sign-Up")
public record SignupResponse(

        @Schema(description = "Generated unique Arcadia user ID", example = "user-8f42b1")
        String userId,

        @Schema(description = "Registered username", example = "zidan.ali")
        String username,

        @Schema(description = "Registered email address", example = "zidan.ali@arcadia.com")
        String email,

        @Schema(description = "Full display name", example = "Zidan Ali")
        String fullName,

        @Schema(description = "Assigned platform role", example = "ROLE_DRIVER")
        Role role,

        @Schema(description = "Assigned vehicle VIN (if provided)", example = "MAKZ1234567890")
        String assignedVin,

        @Schema(description = "Account creation timestamp", example = "2026-10-28T15:30:00Z")
        Instant createdAt,

        @Schema(description = "Human-readable registration message", example = "Account registered successfully. Please log in to obtain your session tokens.")
        String message
) {
}