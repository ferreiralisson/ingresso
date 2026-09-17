package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;

public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    UsuarioPerfil perfil
) {
}
