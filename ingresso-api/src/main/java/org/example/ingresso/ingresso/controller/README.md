# Controllers da API

Expõem contratos HTTP e delegam autenticação, eventos/ofertas e gestão de usuário para serviços Spring.

## Funcionalidades e dependências

- `AuthController`: `POST /api/auth/login`; valida `CredentialRequest`, delega autenticação ao `AuthenticationManager` e emite token via `TokenService`.
- `UsuarioController`: `POST /api/usuarios` cria conta; `GET /api/usuarios` lista usuários paginados e exige ADMIN.
- `PublicEventController`: `GET /api/eventos` e `GET /api/eventos/{id}` expõem catálogo/detalhe públicos; detalhe cancelado permanece visível com status.
- `ProducerEventController`: produtor autenticado lista/cria/edita seus eventos e reativa evento suspenso; ownership é verificado pela API.
- `AdminEventController`: ADMIN lista eventos e suspende/cancela eventos globalmente.
- `PurchaseOrderController`: comprador autenticado cria pedido com `Idempotency-Key`, inicia o mock e consulta apenas o próprio histórico/detalhe, que inclui seus QR após pagamento.
- `EntryValidationController`: operador autorizado valida QR online, provisiona manifesto/sincroniza scans offline; ADMIN consulta auditoria e corrige check-in.
- `EventStaffController`: produtor dono ou ADMIN gerencia grants por evento; convite é aceito por identidade autenticada vinculada ao e-mail.
- `TicketRefundController`: comprador pede refund de ticket próprio; producer/Admin consultam relatórios por event/global em controllers de finance.
- Dependem dos DTOs em `dto`, serviços em `service`/`service.impl` e regras de autorização em `config`/Spring Security.

## Pontos críticos e limites

O cadastro público ignora `perfil`; papéis nunca dependem do formulário web. Cancelamentos producer/Admin acionam refunds simulados para tickets pagos não usados; nenhuma rota movimenta dinheiro. As regras HTTP e erros globais estão em `config`.