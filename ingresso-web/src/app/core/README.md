# Core do frontend

Contém infraestrutura transversal do aplicativo, sem regras de uma feature específica.

- `http/api-url.ts` define o token `API_URL`, com valor padrão `/api`.
- `auth/` contém `AuthService`, guard genérico, interceptor e testes de sessão.
- O interceptor envia Bearer apenas para chamadas privadas à API e encerra uma sessão inválida em `401`.
- O guard genérico protege rotas autenticadas; exceções próprias de funcionalidades, como acesso offline de entrada, ficam na feature correspondente.

Clientes HTTP e contratos de eventos, pedidos, entrada e finanças pertencem a `features/*/data-access` e `features/*/models`.
