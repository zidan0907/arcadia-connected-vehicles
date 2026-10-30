package com.arcadia.auth.api.security;

import com.arcadia.auth.api.domain.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_VIN = "vin";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${arcadia.security.jwt.secret:QXJjYWRpYUNvbm5lY3RlZFZlaGljbGVzU2VjcmV0S2V5MjAyNlNwcmluZ0Jvb3QzTW9kdWxhck1vbm9saXRo}")
    private String jwtSecret;

    @Value("${arcadia.security.jwt.access-expiration-ms:900000}") // 15 minutes
    private long accessExpirationMs;

    @Value("${arcadia.security.jwt.refresh-expiration-ms:604800000}") // 7 days
    private long refreshExpirationMs;

    @Value("${arcadia.security.jwt.issuer:arcadia-connected-vehicles}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes) long.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserAccount user) {
        return buildToken(user, accessExpirationMs, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(UserAccount user) {
        return buildToken(user, refreshExpirationMs, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(UserAccount user, long expirationMs, String tokenType) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getUserId());
        claims.put(CLAIM_USERNAME, user.getUsername());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        if (user.getAssignedVin() != null && !user.getAssignedVin().isBlank()) {
            claims.put(CLAIM_VIN, user.getAssignedVin());
        }

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getUserId())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Duration getRemainingValidity(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            long remainingMillis = expiration.getTime() - System.currentTimeMillis();
            return remainingMillis > 0 ? Duration.ofMillis(remainingMillis) : Duration.ZERO;
        } catch (JwtException | IllegalArgumentException ex) {
            return Duration.ZERO;
        }
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000L;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000L;
    }
}