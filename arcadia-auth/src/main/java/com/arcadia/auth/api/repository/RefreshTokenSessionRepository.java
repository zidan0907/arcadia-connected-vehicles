package com.arcadia.auth.api.repository;

import com.arcadia.auth.api.domain.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, UUID> {

    Optional<RefreshTokenSession> findByTokenJti(String tokenJti);

    @Modifying
    @Query("""
            UPDATE RefreshTokenSession r
            SET r.revoked = true, r.revokedAt = :now
            WHERE r.tokenJti = :tokenJti AND r.revoked = false
            """)
    int revokeByTokenJti(@Param("tokenJti") String tokenJti, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE RefreshTokenSession r
            SET r.revoked = true, r.revokedAt = :now
            WHERE r.userId = :userId AND r.revoked = false
            """)
    int revokeAllActiveByUserId(@Param("userId") String userId, @Param("now") Instant now);
}