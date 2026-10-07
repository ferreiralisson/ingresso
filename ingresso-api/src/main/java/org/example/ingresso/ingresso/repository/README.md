# Repositórios

Camada de persistência baseada em Spring Data JPA para usuários, convites e eventos.

## Dependências e fluxos

- Depende de `model.Usuario`, Spring Data JPA e do datasource configurado em `src/main/resources/application.properties`.
- `UsuarioServiceImpl` usa `findByEmail`, `save` e paginação `findAll(Pageable)`.
- `UserDetailsServiceImpl` usa `findByEmail` durante autenticação e carregamento de Bearer.
- `EventRepository` pagina catálogo por status, lista eventos por produtor e consulta ownership para impedir edição cruzada.
- `TicketCategoryRepository` e `EventSeatRepository` oferecem locks pessimistas para reserva/commit/liberação de inventário.
- `PurchaseOrderRepository` oferece idempotência por comprador/chave, histórico do dono e consulta de pedidos expirados.
- `IssuedTicketRepository` consulta e conta tickets associados a um pedido; ownership é aplicado pelo fluxo autenticado do pedido.
- `EntryCheckInRepository` e `EntryCheckInCorrectionRepository` persistem tentativas e correções auditáveis; `EventStaffInvitationRepository` valida grants ativos; `OfflineEntryManifestRepository` limita hashes ao operador/evento.
- `TicketRefundRepository` garante no máximo um refund por ticket e fornece os registros para reconciliação dos relatórios.
- `ProducerInvitationRepository` trava convites durante aceite/revogação; `PermissionAuditRepository` persiste alterações de papel.

Categorias e mapa de assentos são carregados via agregado `Event`; pedidos, itens e tentativas usam o schema Flyway V4.