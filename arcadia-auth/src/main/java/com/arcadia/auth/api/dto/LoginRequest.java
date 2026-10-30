package com.arcadia.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials payload for signing in on the login page")
public record LoginRequest(

        @Schema(description = "Username, email, or userId", example = "zidan.ali@arcadia.com")
        @NotBlank(message = "Username or email is required")
        @Size(max = 255, message = "Identifier must not exceed 255 characters")
        String usernameOrEmail,

        @Schema(description = "Plaintext account password", example = "Arcadia@2026")
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
        String password
) {
}