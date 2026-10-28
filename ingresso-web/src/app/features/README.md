# Features do frontend

As funcionalidades são organizadas por domínio para manter páginas, contratos e acesso a dados próximos de quem os utiliza. As rotas permanecem lazy-loaded em `../app.routes.ts`.

- `auth`: login, cadastro e conta; modelos/formulários ficam na feature, enquanto sessão, guards genéricos e interceptor permanecem em `core`.
- `events`: catálogo, detalhes, edição do produtor e moderação. `EventApi` atende esses fluxos sem ser duplicado por papel.
- `orders`: checkout, histórico, tickets/QR e refunds simulados.
- `entry`: validação online, manifestos e sincronização offline, equipe e aceite de convite. O guard de acesso offline específico fica nesta feature.
- `finance`: relatório do produtor e visão administrativa dos valores brutos.

Dentro das features, `pages` contém componentes roteáveis, `data-access` contém clientes HTTP e armazenamento local, `models` contém tipos de contratos e `guards` contém regras de navegação específicas da funcionalidade. Componentes reutilizáveis sem regra de domínio ficam em `shared`.
