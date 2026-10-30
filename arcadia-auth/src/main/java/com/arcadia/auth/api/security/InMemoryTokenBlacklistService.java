package com.arcadia.auth.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnProperty(name = "arcadia.security.token-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String jti, Duration ttl) {
        if (StringUtils.hasText(jti) && ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            Instant expiresAt = Instant.now().plus(ttl);
            revokedTokens.put(jti, expiresAt);
            log.debug("Blacklisted JWT in local memory store: jti={}, expiresAt={}", jti, expiresAt);
        }
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return true;
        }
        Instant expiresAt = revokedTokens.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            revokedTokens.remove(jti);
            return false;
        }
        return true;
    }

    /**
     * Purges naturally expired JTIs from memory every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    public void evictExpiredTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}