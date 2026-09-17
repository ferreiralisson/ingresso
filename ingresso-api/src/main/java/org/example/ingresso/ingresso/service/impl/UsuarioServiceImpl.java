package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.dto.CreateUsuarioRequest;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void criar(CreateUsuarioRequest request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            throw new IllegalArgumentException("Email ja cadastrado");
        });

        var perfil = UsuarioPerfil.ADMIN.name().equals(request.perfil()) ? UsuarioPerfil.ADMIN : UsuarioPerfil.USER;

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.password()),
                perfil
        );

        usuarioRepository.save(usuario);
    }
}
