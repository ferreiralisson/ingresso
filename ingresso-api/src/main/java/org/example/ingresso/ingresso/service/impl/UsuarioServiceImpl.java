package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.service.UsuarioService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.CreateUserCommand;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.UserView;
import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public void criar(CreateUserCommand request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            throw new IllegalArgumentException("Email ja cadastrado");
        });

        Usuario usuario = new Usuario(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
            UsuarioPerfil.USER
        );

        usuarioRepository.save(usuario);
    }

    @Override
    public PageResult<UserView> listarUsuarios(PageQuery query) {
        Pageable pageable = toPageable(query);
        Page<Usuario> page = usuarioRepository.findAll(pageable);
        var usuarios = page
                .stream()
                .map(usuario -> new UserView(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getPerfis().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
                ))
                .toList();
        return new PageResult<>(
            usuarios,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            query.sort()
        );
    }

    private Pageable toPageable(PageQuery query) {
        Sort sort = Sort.by(query.sort().stream()
            .map(order -> new Sort.Order(
                order.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                order.property()
            ))
            .toList());
        return PageRequest.of(query.pageNumber(), query.pageSize(), sort);
    }
}
