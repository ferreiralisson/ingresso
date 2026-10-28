# Serviços da API

Camada de aplicação entre controllers, persistência, codificação de senha e Spring Security.

## Contratos e implementações

- Interfaces públicas de serviços ficam neste pacote; implementações e adapters concretos ficam em `service.impl`.
- Controllers dependem apenas das interfaces. Chamadas entre serviços também usam interfaces, sem dependência de uma implementação concreta.
- Interfaces recebem IDs, comandos e consultas próprios da aplicação e retornam DTOs de resultado. Esses contratos ficam em `service.dto` e não expõem entidades JPA, tipos HTTP, `UserSS`, `Pageable` ou repositories.
- Implementações podem usar entidades e repositories internamente. A conversão entre DTO HTTP e DTO de aplicação ocorre em adapters da camada `controller`.
- Os gateways (`PaymentGateway`, `RefundGateway` e `EmailGateway`) são interfaces; mocks e adapters concretos ficam em `service.impl`.
- `ServiceBoundaryTest` usa ArchUnit para impedir dependências de controllers em `service.impl`, dependências indevidas nos contratos e chamadas entre implementações concretas.

## Implementações e dependências

- `UsuarioService` declara criação e listagem paginada; `impl.UsuarioServiceImpl` consulta/salva `Usuario`, aplica BCrypt e mapeia resultados para `UsuarioResponse`.
- `impl.UserDetailsServiceImpl` localiza usuário por e-mail e constrói `UserSS` para autenticação e filtro JWT.
- `EventService` valida campos/categorias/mapas, cria e edita eventos do produtor autenticado, expõe catálogo público, calcula disponibilidade configurada e executa suspensão, cancelamento e reativação.
- `PurchaseOrderService` cria pedido idempotente, trava evento/categorias/assentos, reserva por 15 minutos, processa `PaymentGateway`, expira/libera estoque e serve histórico por comprador.
- `PaymentGateway` é a fronteira substituível; `MockPaymentGateway` é ativo somente em `dev`/`test` e resultado vem de configuração/fixture, nunca do body do comprador.
- `TicketDeliveryService` emite um QR opaco por unidade em pedido pago e inclui tickets apenas nas respostas autorizadas do pedido.
- `EmailGateway` recebe confirmação com tickets após commit; `MockEmailGateway` captura mensagens em `dev`/`test`, e falhas são registradas sem retry automático.
- `EventStaffService` gerencia convite 48h, aceite autenticado e revogação de grants por evento, sem criar role global.
- `EntryValidationService` valida online no dia local de evento publicado, serializa uso de QR, entrega manifesto offline de 24h (somente hashes), sincroniza de forma idempotente e registra correções ADMIN.
- `TicketRefundService` solicita refund mock por ticket não usado antes do evento e automatiza refunds elegíveis em cancelamento; `RefundGateway` delimita a integração e o mock não movimenta dinheiro.
- `FinancialReportService` reconcilia vendas brutas, refunds simulados e bruto restante por evento ou plataforma; não calcula comissão ou repasse.
- Dependências internas: `repository`, `model`, `dto`, `security`; dependências externas: Spring Data, Spring Security e `PasswordEncoder`.

## Observações técnicas

O cadastro público ignora o perfil recebido no request e sempre atribui `USER`; produtor só é concedido pelo aceite de convite ADMIN. O perfil do cliente nunca controla autorização.

O catálogo contém eventos `PUBLISHED`; eventos suspensos não são expostos publicamente, enquanto cancelados mantêm o detalhe e deixam ofertas indisponíveis. Suspender/cancelar também encerra pedidos pendentes e libera reservas; pedidos pagos permanecem pagos.

A listagem de usuários usa o total do `Page` devolvido pelo repositório ao montar o resultado de aplicação; os metadados de paginação refletem o total real, não apenas o conteúdo da página carregada.