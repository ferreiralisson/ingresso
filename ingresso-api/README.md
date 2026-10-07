# ingresso-api

API REST Spring Boot para contas/segurança, eventos/ofertas, pedidos/tickets QR, operação de entrada e pós-venda mock.

## Execução e validação

Requisitos: Java 21 e Maven (ou wrapper incluído).

```sh
export TOKEN_SECRET="$(openssl rand -hex 32)"
./mvnw spring-boot:run
./mvnw test
```

Configure `TOKEN_SECRET` com ao menos 64 bytes UTF-8 antes de iniciar; a API falha no startup se estiver ausente ou curto. O datasource padrão é H2 em arquivo (`./data/ingresso` relativo ao módulo), e Flyway aplica migrations antes de Hibernate validar o schema. O console H2 em `/h2-console` fica desabilitado por padrão; habilite-o somente em ambiente local confiável. Swagger/OpenAPI fica em `/swagger-ui.html` e `/v3/api-docs`.

Configuração disponível por ambiente: `TOKEN_SECRET`, `INGRESSO_DB_URL`, `INGRESSO_DB_USERNAME`, `INGRESSO_DB_PASSWORD`, `H2_CONSOLE_ENABLED`, `JPA_SHOW_SQL`, `PORT` e `INGRESSO_PAYMENT_MOCK_RESULT` (`APPROVED`, `DECLINED` ou `PENDING` em `dev`/`test`). O resultado não é aceito no payload do comprador. Consulte [`manual-admin-provisioning.md`](../specs/001-foundation-security/manual-admin-provisioning.md) para criar o primeiro administrador.

## Responsabilidades e dependências

- `controller`: contratos HTTP de autenticação, eventos, pedidos, equipe e validação de entrada; depende de services, DTOs e Spring Security.
- `service` e `service.impl`: contas, eventos, pedidos/tickets, grants event-scoped, check-in, refunds simulados e relatórios brutos.
- `repository`: consultas JPA de contas, eventos, pedidos, tickets, grants, manifests, check-ins e refunds.
- `model`: usuários/papéis, eventos/ofertas, pedidos/tickets, entrada, refunds e correções.
- `dto`: contratos de requisição/resposta validados.
- `security`: emissão/verificação JWT, filtro stateless e principal Spring Security.
- `config`: cadeia de segurança, CORS, constantes de papel, handler de exceção e OpenAPI.

Dependências principais: Spring Boot 4.1.1, Java 21, Spring MVC, Security, Validation, Data JPA, Flyway, H2, Auth0 `java-jwt` 4.4.0 e Springdoc 2.8.13. Escopos completos em `pom.xml`.

## Entradas e fluxos

- Entrada de processo: `src/main/java/org/example/ingresso/ingresso/IngressoApiApplication.java`.
- `POST /api/usuarios`: público; valida request, verifica e-mail, codifica senha, salva usuário e retorna `201`.
- `POST /api/auth/login`: público; autentica e responde `{ "token": "..." }`.
- `GET /api/usuarios`: autenticado e restrito a ADMIN; aceita paginação Spring Data.
- `GET /api/eventos` e `GET /api/eventos/{id}`: catálogo/detalhe público; eventos cancelados mantêm detalhe com aviso, suspensos retornam não encontrado.
- `GET/POST /api/produtor/eventos`, `GET/PUT /api/produtor/eventos/{id}`: lista, cria/publica e edita eventos do produtor autenticado; `PATCH .../{id}/reativar` reativa evento suspenso do dono.
- `GET /api/admin/eventos`, `GET /api/admin/eventos/{id}`, `PATCH .../{id}/suspender` e `PATCH .../{id}/cancelar`: moderação ADMIN.
- Ofertas usam BRL em centavos; assentos são gerados por setores/fileiras e cada lugar referencia uma categoria.
- `POST /api/pedidos`: cria pedido autenticado de um evento; exige `Idempotency-Key` e reserva estoque por até 15 minutos ou até o início do evento.
- `POST /api/pedidos/{id}/pagamento`: inicia uma tentativa no `PaymentGateway`; o mock responde aprovado/recusado/pendente sem campo de resultado no pedido do comprador.
- `GET /api/pedidos` e `GET /api/pedidos/{id}`: histórico e detalhe restritos ao dono do pedido.
- Pedido `PAID` inclui um ticket/QR opaco por unidade em `tickets`; a resposta é acessível somente ao comprador dono do pedido.
- `POST /api/pedidos/{orderId}/tickets/{ticketId}/reembolsos`: solicita refund simulado por ticket pago/não usado antes do evento; usa preço snapshot e é idempotente.
- `PATCH /api/produtor/eventos/{id}/cancelar` ou `PATCH /api/admin/eventos/{id}/cancelar`: cancelamento cria refund mock para tickets pagos/não usados.
- `GET /api/produtor/eventos/{id}/financeiro` e `GET /api/admin/financeiro`: valores brutos, refunds simulados e restante; não calculam comissão/repasse.
- `POST /api/entrada/eventos/{id}/validar`: produtor dono, equipe com grant aceito ou ADMIN valida ticket; uso e auditoria são atômicos.
- `GET /api/entrada/eventos/{id}/manifesto-offline` e `POST /api/entrada/eventos/{id}/sincronizar`: manifesto hash-only com lease de 24 horas; sync é idempotente e revalida acesso/evento/ticket.
- `GET /api/entrada/eventos/{id}/auditoria` e `POST /api/entrada/auditoria/{checkInId}/corrigir`: auditoria/correção restritas a ADMIN; correção reabre o ticket uma vez.
- `POST/GET/DELETE /api/entrada/eventos/{id}/equipe`: convite por evento, grants aceitos e revogação; `POST /api/entrada/convites/equipe/aceitar` exige identidade autenticada igual ao e-mail do convite. Token expira em 48 horas.
- `POST /api/admin/convites/produtores`: ADMIN cria convite para e-mail; resposta contém token em texto claro uma única vez, para entrega segura fora da aplicação.
- `POST /api/convites/produtores/aceitar`: usuário autenticado aceita convite vinculado ao próprio e-mail; token expira em 48 horas e só pode ser usado uma vez.
- `DELETE /api/admin/convites/produtores/{id}` revoga convite pendente; `DELETE /api/admin/produtores/{userId}` remove somente o papel PRODUCER.
- Demais rotas exigem autenticação na configuração de segurança. O filtro recupera Bearer, valida subject e carrega usuário no repositório.

O web relacionado está em `../ingresso-web`; contratos e proxy estão documentados no [README do frontend](../ingresso-web/README.md). Arquitetura completa e riscos estão em [Arquitetura do Sistema](../Arquitetura%20do%20Sistema.md).

## Arquivos críticos e observações

- `src/main/resources/application.properties`: configuração por ambiente; segredo sem valor padrão; H2 persistente e console opt-in.
- `src/main/resources/db/migration`: migrações Flyway. A migração converte perfis legados e rebaixa ADMIN anterior a USER, com auditoria, pois o cadastro antigo permitia autoelevação.
- `config/SecurityConfig.java`, `security/SecurityFilter.java`, `security/TokenService.java`: fronteira de segurança.
- `controller/AdminProducerController.java`, `controller/ProducerInvitationController.java`, `service/ProducerInvitationService.java`: convite, aceite, revogação e auditoria.
- `controller/PublicEventController.java`, `controller/ProducerEventController.java`, `controller/AdminEventController.java`, `service/EventService.java`: domínio, ownership, catálogo e ciclo de vida.
- `model/Event.java`, `model/TicketCategory.java`, `model/SeatSector.java`, `model/SeatRow.java`, `model/EventSeat.java`: agregado de evento e mapa de lugares.
- `src/main/resources/db/migration/V3__events_and_offers.sql`: schema para eventos, categorias e assentos.
- `src/main/resources/db/migration/V4__orders_and_mock_payments.sql` até `V11__ticket_refunds.sql`: pedidos, tickets, entrada, manifestos hash-only e refunds únicos por ticket.
- `service/PurchaseOrderService.java`, `service/PaymentGateway.java`, `service/MockPaymentGateway.java`: locks de inventário, expiração e gateway mock substituível.
- `service/TicketDeliveryService.java`, `service/EmailGateway.java`, `service/MockEmailGateway.java`: emissão idempotente e confirmação mock após commit; falhas são registradas sem retry automático.
- `service/EntryValidationService.java`, `service/EventStaffService.java`: uso único concorrente, janela/status do evento, grants por evento, sync offline e correções ADMIN.
- `service/TicketRefundService.java`, `service/RefundGateway.java`, `service/MockRefundGateway.java`: refund simulado por ticket sob lock, preço snapshot e cancelamento automático; não movimenta dinheiro.
- `service/FinancialReportService.java`: agregados de vendas brutas/refunds por evento e plataforma, sem comissão/net payable.
- Testes cobrem cadastro, convites, migrações, eventos, concorrência de pedidos, idempotency key, estados do mock, aprovação tardia e ofertas protegidas contra reservas pendentes.
- A API aceita o campo legado `perfil` no cadastro por compatibilidade, mas o ignora; contas públicas recebem apenas USER.
- Risco: `PageImpl` recebe tamanho da página como total; corrigir antes de depender dos metadados totais.
- H2 em arquivo é uma escolha de persistência local/MVP, não uma configuração de produção multi-instância.
- O primeiro ADMIN é provisionado manualmente no banco; a API não tem rota pública ou administrativa para criar ADMIN. O token de convite de produtor precisa ser entregue fora de banda; envio de e-mail não faz parte desta feature.
- Pagamento/refund e e-mail reais ainda não existem; estados de refund são `SIMULATED`. Comissão, payout e política de ticket usado em cancelamento seguem pendentes.
- Mock disponível apenas em profiles `dev`/`test`; `INGRESSO_PAYMENT_MOCK_RESULT` configura o resultado padrão local. Nenhuma rota/campo permite ao comprador definir o outcome.
- Débito: `nome` tem restrição de unicidade no banco sem tratamento específico; `SECURITY_ROLE_USER` permanece sem referência.