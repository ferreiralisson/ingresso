# Páginas do frontend

Contém as telas Angular lazy-loaded associadas às rotas em `../app.routes.ts`.

## Páginas e fluxos

- `login.ts` / `login.html`: valida e-mail/senha, chama `AuthService.login` e navega à conta; trata indicação de cadastro concluído e sessão expirada.
- `register.ts` / `register.html`: valida nome, e-mail, senha e confirmação; envia cadastro sem papel definido pelo cliente e navega ao login em sucesso.
- `account.ts`: exibe e-mail e expiração do token disponíveis localmente e permite sair. Não consulta nome/perfil nem API de perfil.
- `events.ts` / `event-detail.ts`: lista pública paginada e detalhe com ofertas, quantidade configurada e mapa de assentos.
- `producer-events.ts` / `event-editor.ts`: lista, cria/edita evento, categorias, setores, fileiras e assentos; o servidor decide o acesso do produtor.
- `admin-events.ts`: lista eventos para ADMIN e chama suspensão/cancelamento; a API valida a role.
- `checkout.ts`: checkout de evento único, seleção de quantidade/assentos, total BRL, chave idempotente e início do mock; não permite selecionar o resultado.
- `orders.ts`: histórico autenticado com status, totais, snapshots dos itens e ingressos/QR após pagamento; `ticket-qr.ts` renderiza tokens como SVG QR.
- `financial-report.ts`: mostra valores brutos/refunds simulados por evento do produtor ou agregado ADMIN; não calcula comissão nem repasse.
- `entry-validation.ts`: câmera ZXing/manual, validação online, manifesto/hash offline, scans provisórios e sync; `event-staff-manager.ts` administra grants do evento.
- `accept-event-staff-invite.ts`: aceite autenticado de convite event-scoped.
- O histórico permite refund mock por ticket elegível, oculta o QR após refund e informa que nenhum dinheiro foi movimentado.

Dependem de `../core` para sessão, contratos e armazenamento offline, de `../shared/auth-layout` para composição das telas de autenticação e de Angular Router. O service worker cacheia a shell no build de produção; ingressos offline permanecem provisórios até resposta do servidor.