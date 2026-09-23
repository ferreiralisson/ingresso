package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.dto.CreateUsuarioRequest;
import org.example.ingresso.ingresso.dto.UsuarioResponse;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        var usuarios = usuarioRepository.findAll(pageable)
                .stream()
                .map(usuario -> new UsuarioResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getUsuarioPerfil()
                ))
                .toList();


        return new PageImpl<>(usuarios, pageable, usuarios.size());
    }
}
