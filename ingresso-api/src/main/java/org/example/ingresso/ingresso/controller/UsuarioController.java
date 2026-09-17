package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateUsuarioRequest;
import org.example.ingresso.ingresso.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
