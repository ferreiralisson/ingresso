package org.example.ingresso.ingresso.security;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserSS implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UsuarioPerfil usuarioPerfil;

    public UserSS(Long id, String email, String password, UsuarioPerfil usuarioPerfil) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.usuarioPerfil = usuarioPerfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (usuarioPerfil == UsuarioPerfil.ADMIN) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
