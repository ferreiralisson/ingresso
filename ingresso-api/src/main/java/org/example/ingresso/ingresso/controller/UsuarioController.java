package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateUsuarioRequest;
import org.example.ingresso.ingresso.dto.UsuarioResponse;
import org.example.ingresso.ingresso.service.UsuarioService;
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
        usuarioService.criar(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize(SECURITY_ROLE_ADMIN)
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(Pageable pageable) {
        var usuarios = usuarioService.listarUsuarios(pageable);
        return ResponseEntity.ok(usuarios);
    }
}
