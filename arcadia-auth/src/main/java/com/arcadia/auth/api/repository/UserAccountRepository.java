package com.arcadia.auth.api.repository;

import com.arcadia.auth.api.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByUserId(String userId);

    @Query("""
            SELECT u FROM UserAccount u
            WHERE LOWER(u.username) = LOWER(:identifier)
               OR LOWER(u.email) = LOWER(:identifier)
               OR LOWER(u.userId) = LOWER(:identifier)
            """)
    Optional<UserAccount> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUserId(String userId);
}