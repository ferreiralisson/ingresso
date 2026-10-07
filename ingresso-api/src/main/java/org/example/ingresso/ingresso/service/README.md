# Serviços da API

Camada de aplicação entre controllers, persistência, codificação de senha e Spring Security.

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

A paginação cria `PageImpl` usando `usuarios.size()` como total, em vez do total do `Page` devolvido por `findAll(pageable)`. Manter regras de negócio aqui e acrescentar testes de perfil, duplicidade e metadados de paginação.