package org.example.ingresso.ingresso.security;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class UserSS implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Set<UsuarioPerfil> perfis;

    public UserSS(Long id, String email, String password, Collection<UsuarioPerfil> perfis) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.perfis = Set.copyOf(perfis);
    }

    public UserSS(Long id, String email, String password, UsuarioPerfil usuarioPerfil) {
        this(id, email, password, List.of(usuarioPerfil));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return perfis.stream()
            .map(perfil -> new SimpleGrantedAuthority("ROLE_" + perfil.name()))
            .toList();
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
