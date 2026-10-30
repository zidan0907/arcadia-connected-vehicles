package com.arcadia.auth.api.security;

import java.time.Duration;

public interface TokenBlacklistService {

    /**
     * Blacklists a JWT ID (jti) for its remaining validity window.
     */
    void blacklistToken(String jti, Duration ttl);

    /**
     * Returns true if the given JWT ID (jti) has been revoked.
     */
    boolean isTokenBlacklisted(String jti);
}