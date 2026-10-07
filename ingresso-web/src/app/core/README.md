# Core do frontend

Concentra contratos da API e comportamento transversal: `api.ts`, `event-api.ts`, `order-api.ts`, `entry-api.ts`, `finance-api.ts`, `entry-offline-store.ts`, auth, guards, interceptor e validações.

## Responsabilidades e fluxos

- Define `API_URL` padrão `/api` e tipos de credenciais, cadastro, resposta e sessão.
- `EventApi` tipa e chama catálogo público, detalhe, gestão de produtor, mapa/ofertas e moderação ADMIN.
- `OrderApi` cria pedidos com `Idempotency-Key`, inicia pagamento mock e lista/consulta pedidos autenticados, incluindo tickets/QR após aprovação.
- `EntryApi` valida ticket, provisiona manifestos, sincroniza scans, lista auditoria e administra grants event-scoped.
- `FinanceApi` chama relatórios producer/Admin de valores brutos; `OrderApi` solicita refund simulado para um ticket do próprio pedido.
- `EntryOfflineStore` guarda no IndexedDB hashes QR e fila local; manifests ficam ligados ao e-mail do operador e expiram em 24 horas.
- `AuthService` chama login/cadastro, decodifica claims para UI, restaura e armazena sessão em `sessionStorage` com fallback em memória e agenda expiração.
- Guards protegem rotas privadas; a rota de entrada aceita lease local ainda válido após expiração JWT para scans provisórios, mas sync exige autenticação renovada.
- Interceptor envia Bearer apenas em chamadas privadas para a mesma API e encerra a sessão em `401` privado.
- Utilitários validam campos/senhas e convertem erros em mensagens.

## Dependências e pontos críticos

Depende de Angular HTTP/router, RxJS e IndexedDB; ZXing e service worker atendem câmera e shell offline. A API permanece autoridade de operador, evento, ticket, uso e conflito. Testes cobrem contratos de páginas e auth.