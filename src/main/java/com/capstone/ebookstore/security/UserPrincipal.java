package com.capstone.ebookstore.security;

import com.capstone.ebookstore.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Thin wrapper around {@link User} that implements Spring Security's {@link UserDetails}.
 * Keeps the JPA entity free of Spring Security concerns and avoids CGLIB proxy recursion.
 */
@RequiredArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired()    { return true; }

    @Override
    public boolean isAccountNonLocked()     { return true; }

    @Override
    public boolean isCredentialsNonExpired(){ return true; }

    @Override
    public boolean isEnabled()              { return true; }

    /** Convenience accessor used in controllers. */
    public Long getId()        { return user.getId(); }
    public String getEmail()   { return user.getEmail(); }
}
