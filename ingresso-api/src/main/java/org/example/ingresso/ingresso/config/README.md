# Configuração da API

Este pacote centraliza configuração transversal do Spring: cadeia de autorização/CORS/CSRF/sessão, codificador BCrypt, manager de autenticação, tratamento REST de `IllegalArgumentException`, documentação OpenAPI e constantes de papéis.

## Dependências e relações

Depende de Spring Security, Spring Web e Springdoc. `SecurityConfig` instala `SecurityFilter` do pacote `security`; controllers usam as constantes para autorização. `application.properties`, fora deste pacote, fornece propriedades operacionais como porta, datasource e segredo.

## Entradas e arquivos críticos

- `SecurityConfig.java`: política HTTP; login e cadastro são públicos, demais rotas autenticadas; `GET /api/usuarios` aplica adicionalmente regra ADMIN no controller.
- `ApiExceptionHandler.java`: converte `IllegalArgumentException` em HTTP 409.
- `OpenApiConfig.java`: configura bearer JWT no OpenAPI.
- `Constants.java`: nomes e expressões dos papéis. `SECURITY_ROLE_USER` não tem uso encontrado.

## Observações

CORS usa valores padrão permitidos e métodos explicitados no código; não há allowlist de origem dedicada. A configuração deve ser revisada por ambiente antes de publicação. Consulte o README na raiz de `ingresso-api` para o módulo completo.