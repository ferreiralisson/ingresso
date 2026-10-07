package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;

import java.util.Set;

public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    Set<UsuarioPerfil> perfis
) {
}
