package com.documentanalysis.repository;

import com.documentanalysis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.verificationToken = :token AND u.verificationTokenExpires > :now")
    Optional<User> findByVerificationTokenAndNotExpired(@Param("token") String token, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.resetToken = :token AND u.resetTokenExpires > :now")
    Optional<User> findByResetTokenAndNotExpired(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.email = :email")
    int updateLastLoginByEmail(@Param("email") String email, @Param("lastLogin") LocalDateTime lastLogin);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.email = :email")
    int updateFailedLoginAttempts(@Param("email") String email, @Param("attempts") int attempts);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.email = :email")
    int lockUser(@Param("email") String email, @Param("lockedUntil") LocalDateTime lockedUntil);
}