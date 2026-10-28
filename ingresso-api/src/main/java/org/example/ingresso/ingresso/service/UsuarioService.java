package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.CreateUserCommand;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.UserView;
import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;

public interface UsuarioService {
    void criar(CreateUserCommand request);

    PageResult<UserView> listarUsuarios(PageQuery query);
}
