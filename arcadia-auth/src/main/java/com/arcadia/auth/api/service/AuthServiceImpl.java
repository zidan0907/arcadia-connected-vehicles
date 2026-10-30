package com.arcadia.auth.api.service;

import com.arcadia.auth.api.domain.RefreshTokenSession;
import com.arcadia.auth.api.domain.Role;
import com.arcadia.auth.api.domain.UserAccount;
import com.arcadia.auth.api.dto.AuthResponse;
import com.arcadia.auth.api.dto.LoginRequest;
import com.arcadia.auth.api.dto.RefreshTokenRequest;
import com.arcadia.auth.api.dto.SignupRequest;
import com.arcadia.auth.api.dto.SignupResponse;
import com.arcadia.auth.api.dto.UserProfileResponse;
import com.arcadia.auth.api.repository.RefreshTokenSessionRepository;
import com.arcadia.auth.api.repository.UserAccountRepository;
import com.arcadia.auth.api.security.CookieUtils;
import com.arcadia.auth.api.security.JwtAuthenticationFilter;
import com.arcadia.auth.api.security.JwtTokenProvider;
import com.arcadia.auth.api.security.TokenBlacklistService;
import com.arcadia.common.error.ErrorCode;
import com.arcadia.common.exceptions.ResourceConflictException;
import com.arcadia.common.exceptions.ResourceNotFoundException;
import com.arcadia.common.exceptions.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final CookieUtils cookieUtils;

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String normalizedUsername = request.username().trim();
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userAccountRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResourceConflictException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Username '" + normalizedUsername + "' is already taken."
            );
        }

        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Email '" + normalizedEmail + "' is already registered."
            );
        }

        String generatedUserId = generateUniqueUserId();
        Role assignedRole = request.role() != null ? request.role() : Role.ROLE_DRIVER;
        String cleanVin = StringUtils.hasText(request.assignedVin()) ? request.assignedVin().trim().toUpperCase() : null;

        UserAccount newUser = UserAccount.builder()
                .userId(generatedUserId)
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(assignedRole)
                .assignedVin(cleanVin)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        UserAccount saved = userAccountRepository.save(newUser);
        log.info("Registered new user account: userId={}, username={}, role={}",
                saved.getUserId(), saved.getUsername(), saved.getRole());

        return SignupResponse.builder()
                .userId(saved.getUserId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .assignedVin(saved.getAssignedVin())
                .createdAt(saved.getCreatedAt())
                .message("Account registered successfully. Please log in to obtain your session tokens.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UserAccount user = userAccountRepository.findByIdentifier(request.usernameOrEmail().trim())
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "Invalid username/email or password."
                ));

        if (!user.isEnabled()) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_DISABLED, "User account is disabled.");
        }

        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_LOCKED, "User account is locked.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for identifier={}", request.usernameOrEmail());
            throw new UnauthorizedException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid username/email or password."
            );
        }

        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);

        log.info("User signed in successfully: userId={}, role={}", user.getUserId(), user.getRole());
        return issueTokensAndCookies(user, httpRequest, httpResponse);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(
            RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawRefreshToken = resolveRefreshToken(request, httpRequest);
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new UnauthorizedException(
                    ErrorCode.TOKEN_INVALID,
                    "Refresh token is required in request body or HttpOnly cookie."
            );
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(rawRefreshToken);
        } catch (ExpiredJwtException ex) {
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired.");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID, "Invalid or malformed refresh token.");
        }

        String tokenType = claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE, String.class);
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new UnauthorizedException(
                    ErrorCode.TOKEN_INVALID,
                    "Provided token is not a valid refresh token."
            );
        }

        String jti = claims.getId();
        if (tokenBlacklistService.isTokenBlacklisted(jti)) {
            throw new UnauthorizedException(
                    ErrorCode.TOKEN_REVOKED,
                    "Refresh token has already been revoked."
            );
        }

        // Check PostgreSQL refresh_tokens table for durable revocation state
        RefreshTokenSession storedSession = refreshTokenSessionRepository.findByTokenJti(jti).orElse(null);
        if (storedSession != null && storedSession.isRevoked()) {
            throw new UnauthorizedException(
                    ErrorCode.TOKEN_REVOKED,
                    "Refresh token session has already been revoked."
            );
        }

        String userId = claims.getSubject();
        UserAccount user = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.USER_NOT_FOUND,
                        "User account no longer exists."
                ));

        if (!user.isEnabled()) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_DISABLED, "User account is disabled.");
        }
        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_LOCKED, "User account is locked.");
        }

        // Rotate refresh token: revoke consumed token in memory/Redis and in PostgreSQL
        revokeJwtToken(rawRefreshToken);
        refreshTokenSessionRepository.revokeByTokenJti(jti, Instant.now());

        log.info("Rotated JWT session tokens for userId={}", user.getUserId());
        return issueTokensAndCookies(user, httpRequest, httpResponse);
    }

    @Override
    @Transactional
    public void logout(
            String authorizationHeader,
            RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        // 1. Revoke Access Token (from Bearer header or HttpOnly cookie)
        String accessToken = null;
        if (StringUtils.hasText(authorizationHeader)
                && authorizationHeader.startsWith(JwtAuthenticationFilter.BEARER_PREFIX)) {
            accessToken = authorizationHeader.substring(JwtAuthenticationFilter.BEARER_PREFIX.length()).trim();
        } else if (httpRequest != null) {
            accessToken = cookieUtils.getCookieValue(httpRequest, CookieUtils.ACCESS_TOKEN_COOKIE_NAME).orElse(null);
        }
        if (StringUtils.hasText(accessToken)) {
            revokeJwtToken(accessToken);
        }

        // 2. Revoke Refresh Token (from JSON body or HttpOnly cookie)
        String refreshToken = resolveRefreshToken(refreshTokenRequest, httpRequest);
        if (StringUtils.hasText(refreshToken)) {
            String refreshJti = revokeJwtToken(refreshToken);
            if (StringUtils.hasText(refreshJti)) {
                refreshTokenSessionRepository.revokeByTokenJti(refreshJti, Instant.now());
            }
        }

        // 3. Clear HttpOnly cookies on response
        if (httpResponse != null) {
            cookieUtils.clearAuthCookies(httpResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String userId) {
        UserAccount user = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "User account '" + userId + "' was not found."
                ));

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .assignedVin(user.getAssignedVin())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse issueTokensAndCookies(
            UserAccount user,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
        RefreshTokenSession session = RefreshTokenSession.builder()
                .tokenJti(refreshClaims.getId())
                .userId(user.getUserId())
                .expiresAt(refreshClaims.getExpiration().toInstant())
                .revoked(false)
                .userAgent(httpRequest != null ? httpRequest.getHeader("User-Agent") : null)
                .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : null)
                .build();
        refreshTokenSessionRepository.save(session);

        if (httpResponse != null) {
            cookieUtils.addAuthCookies(
                    httpResponse,
                    accessToken,
                    jwtTokenProvider.getAccessExpirationSeconds(),
                    refreshToken,
                    jwtTokenProvider.getRefreshExpirationSeconds()
            );
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessExpirationSeconds())
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .assignedVin(user.getAssignedVin())
                .issuedAt(Instant.now())
                .build();
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            return request.refreshToken().trim();
        }
        if (httpRequest != null) {
            return cookieUtils.getCookieValue(httpRequest, CookieUtils.REFRESH_TOKEN_COOKIE_NAME).orElse(null);
        }
        return null;
    }

    private String revokeJwtToken(String token) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            String jti = claims.getId();
            Duration ttl = jwtTokenProvider.getRemainingValidity(token);
            tokenBlacklistService.blacklistToken(jti, ttl);
            return jti;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Ignored revocation for expired/malformed token: {}", ex.getMessage());
            return null;
        }
    }

    private String generateUniqueUserId() {
        String candidate;
        do {
            candidate = "user-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (userAccountRepository.existsByUserId(candidate));
        return candidate;
    }
}