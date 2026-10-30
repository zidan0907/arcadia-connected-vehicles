package com.arcadia.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for exchanging a refresh token (can also be supplied via HttpOnly cookie)")
public record RefreshTokenRequest(

        @Schema(
                description = "Signed JWT refresh token (optional in body if arcadia_refresh_token cookie is present)",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String refreshToken
) {
}