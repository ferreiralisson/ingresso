# Segurança e tokens

Implementa principal Spring Security, assinatura/verificação de JWT e filtro HTTP de Bearer.

## Fluxo e dependências

`TokenService` assina/verifica com Auth0 `java-jwt`, HMAC512 e propriedade `token.secret`; usa issuer `ingresso-api`, subject de e-mail e validade de duas horas. `SecurityFilter` extrai `Authorization: Bearer`, valida o token, delega busca a `UserDetailsServiceImpl` no pacote `service.impl` e instala autenticação no contexto Spring. `UserSS` expõe autoridades derivadas de `UsuarioPerfil`.

Integra-se a `config.SecurityConfig`, `UsuarioRepository` por meio do user-details service, e ao `AuthController` no login. O web apenas lê claims para estado visual; assinatura e autorização são responsabilidade do servidor.

## Arquivos críticos e riscos

- `TokenService.java`, `SecurityFilter.java`, `UserSS.java`.
- `TOKEN_SECRET` não tem valor padrão e o startup rejeita menos de 64 bytes UTF-8; fornecer e rotacionar o valor por ambiente sem versioná-lo.
- O subject é e-mail; mudanças no identificador exigem compatibilidade com tokens emitidos e `UserDetailsServiceImpl`.
- Testes encontrados validam geração, leitura e rejeição de token; não cobrem autorização ponta a ponta.