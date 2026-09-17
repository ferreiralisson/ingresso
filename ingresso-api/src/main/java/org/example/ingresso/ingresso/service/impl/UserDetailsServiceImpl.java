package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.UserSS;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(email));

        return new UserSS(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getSenha(),
            usuario.getUsuarioPerfil()
        );
    }
}
