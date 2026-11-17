package ru.mirea.hrsystem.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.mirea.hrsystem.domain.Role;
import ru.mirea.hrsystem.domain.UserAccount;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {
    private final Long id;
    private final UUID uuid;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final boolean mustChangePassword;

    public UserPrincipal(UserAccount user) {
        this.id = user.getId();
        this.uuid = user.getUuid();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.mustChangePassword = user.isMustChangePassword();
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public Role getRole() { return role; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
