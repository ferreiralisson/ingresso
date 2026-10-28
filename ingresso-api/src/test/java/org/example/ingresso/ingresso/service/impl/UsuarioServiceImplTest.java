package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.CreateUserCommand;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void publicRegistrationAlwaysCreatesBuyer() {
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        usuarioService.criar(new CreateUserCommand("Ana", "ana@example.com", "password"));

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertTrue(usuarioCaptor.getValue().getPerfis().contains(UsuarioPerfil.USER));
    }
}