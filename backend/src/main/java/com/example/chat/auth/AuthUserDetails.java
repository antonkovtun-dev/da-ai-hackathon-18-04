package com.example.chat.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class AuthUserDetails implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String email;
    private final String username;
    private final String passwordHash;

    public AuthUserDetails(UUID userId, String email, String username, String passwordHash) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UUID getUserId() { return userId; }
    public String getUserUsername() { return username; }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
