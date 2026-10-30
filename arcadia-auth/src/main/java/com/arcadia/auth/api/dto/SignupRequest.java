package com.arcadia.auth.api.dto;

import com.arcadia.auth.api.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Sign-Up payload for registering a new driver or fleet administrator account")
public record SignupRequest(

        @Schema(description = "Unique login username", example = "zidan.ali")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, digits, dots, underscores, and hyphens")
        String username,

        @Schema(description = "Valid user email address", example = "zidan.ali@arcadia.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(description = "Account password (min 8 characters)", example = "Arcadia@2026")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @Schema(description = "Full display name of the user", example = "Zidan Ali")
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @Schema(description = "Optional platform role (defaults to ROLE_DRIVER)", example = "ROLE_DRIVER")
        Role role,

        @Schema(description = "Optional primary vehicle VIN assigned to the driver", example = "MAKZ1234567890")
        @Size(max = 32, message = "VIN must not exceed 32 characters")
        String assignedVin
) {
}