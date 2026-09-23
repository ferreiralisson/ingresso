package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.CreateUsuarioRequest;
import org.example.ingresso.ingresso.dto.UsuarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {
    void criar(CreateUsuarioRequest request);
    Page<UsuarioResponse> listarUsuarios(Pageable pageable);
}
