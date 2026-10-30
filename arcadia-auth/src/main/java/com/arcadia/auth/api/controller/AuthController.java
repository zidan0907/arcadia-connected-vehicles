package com.arcadia.auth.api.controller;

import com.arcadia.auth.api.dto.AuthResponse;
import com.arcadia.auth.api.dto.LoginRequest;
import com.arcadia.auth.api.dto.RefreshTokenRequest;
import com.arcadia.auth.api.dto.SignupRequest;
import com.arcadia.auth.api.dto.SignupResponse;
import com.arcadia.auth.api.dto.UserProfileResponse;
import com.arcadia.auth.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Session", description = "User sign-up, JWT + HttpOnly cookie login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Sign up and register a new user account",
            description = "Creates a new driver or fleet admin account in PostgreSQL (auth.user_accounts) with a BCrypt-hashed password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User account registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in sign-up payload"),
            @ApiResponse(responseCode = "409", description = "Username or email is already registered")
    })
    @PostMapping({"/signup", "/register"})
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Sign in and obtain JWT tokens + HttpOnly cookies",
            description = "Authenticates credentials against auth.user_accounts, returns signed Access & Refresh JWTs in JSON, and sets HttpOnly cookies."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in login payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials, disabled, or locked account")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @Operation(
            summary = "Rotate refresh token and issue a new JWT pair",
            description = "Accepts a refresh token from either the JSON body or the arcadia_refresh_token HttpOnly cookie, revokes the old token, and issues a fresh pair."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "401", description = "Refresh token expired, malformed, or already revoked")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.ok(authService.refreshToken(request, httpRequest, httpResponse));
    }

    @Operation(
            summary = "Logout and revoke active session tokens & cookies",
            description = "Blacklists the caller's access and refresh tokens, marks the session revoked in PostgreSQL, and clears HttpOnly cookies."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session tokens revoked and cookies cleared")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Bearer access token in Authorization header")
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authService.logout(authorizationHeader, refreshTokenRequest, httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get current authenticated user profile",
            description = "Returns the profile of the currently authenticated user using either the Authorization Bearer header or HttpOnly access cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user profile retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing, expired, or revoked access token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(authService.getCurrentUserProfile(userId));
    }
}