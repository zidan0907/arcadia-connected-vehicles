package com.arcadia.auth.api.dto;

import com.arcadia.auth.api.domain.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authenticated user profile details returned by GET /api/v1/auth/me")
public record UserProfileResponse(
        String userId,
        String username,
        String email,
        String fullName,
        Role role,
        String assignedVin,
        Instant lastLoginAt,
        Instant createdAt
) {
}