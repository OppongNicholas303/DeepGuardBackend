package com.documentanalysis.repository;

import com.documentanalysis.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByTokenHashAndIsRevokedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE UserSession us SET us.isRevoked = true WHERE us.tokenHash = :tokenHash")
    int revokeToken(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE UserSession us SET us.isRevoked = true WHERE us.user.id = :userId")
    int revokeAllUserTokens(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT CASE WHEN COUNT(us) > 0 THEN true ELSE false END FROM UserSession us WHERE us.tokenHash = :tokenHash AND us.isRevoked = true")
    boolean isTokenRevoked(@Param("tokenHash") String tokenHash);
}