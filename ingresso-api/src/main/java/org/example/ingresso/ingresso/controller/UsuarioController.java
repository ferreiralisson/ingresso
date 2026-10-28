package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateUsuarioRequest;
import org.example.ingresso.ingresso.dto.UsuarioResponse;
import org.example.ingresso.ingresso.service.UsuarioService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.CreateUserCommand;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.UserView;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.example.ingresso.ingresso.config.Constants.SECURITY_ROLE_ADMIN;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Void> criar(@RequestBody @Valid CreateUsuarioRequest request) {
        usuarioService.criar(new CreateUserCommand(request.nome(), request.email(), request.password()));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize(SECURITY_ROLE_ADMIN)
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(Pageable pageable) {
        var query = PageHttpMapper.toQuery(pageable);
        var result = usuarioService.listarUsuarios(query);
        var users = result.content().stream().map(UsuarioController::toResponse).toList();
        return ResponseEntity.ok(PageHttpMapper.toPage(new org.example.ingresso.ingresso.service.dto.PageResult<>(
            users, result.pageNumber(), result.pageSize(), result.totalElements(), result.sort()
        )));
    }

    private static UsuarioResponse toResponse(UserView user) {
        return new UsuarioResponse(
            user.id(), user.name(), user.email(),
            user.roles().stream().map(org.example.ingresso.ingresso.model.enums.UsuarioPerfil::valueOf)
                .collect(java.util.stream.Collectors.toSet())
        );
    }
}
