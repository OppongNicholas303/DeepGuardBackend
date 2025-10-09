package DeepQuard.DeepGuardBackend.service.security;

import DeepQuard.DeepGuardBackend.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password") // don't log passwords!
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
}
