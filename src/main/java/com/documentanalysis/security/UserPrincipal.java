package com.documentanalysis.security;

import com.documentanalysis.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private UUID id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private boolean emailVerified;
    private String status;
    private LocalDateTime lockedUntil;

    public UserPrincipal() {}

    public UserPrincipal(UUID id, String email, String password, String firstName, String lastName, 
                        String role, boolean emailVerified, String status, LocalDateTime lockedUntil) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.emailVerified = emailVerified;
        this.status = status;
        this.lockedUntil = lockedUntil;
    }

    public static UserPrincipal create(User user) {
        String roleStr = user.getRole() != null ? user.getRole().toString() : "USER";
        String statusStr = user.getStatus() != null ? user.getStatus().toString() : "ACTIVE";

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                roleStr,
                user.isEmailVerified(),
                statusStr,
                user.getLockedUntil()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return firstName + lastName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !"DELETED".equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        if ("SUSPENDED".equals(status)) {
            return false;
        }
        return lockedUntil == null || LocalDateTime.now().isAfter(lockedUntil);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status) && emailVerified;
    }

    // Getters
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getStatus() { return status; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
}