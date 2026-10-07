# ingresso-web

Front-end Angular de integração com `../ingresso-api`. Inclui catálogo, gestão de eventos, checkout mock, tickets QR, operação de entrada offline e pós-venda com refunds simulados/relatórios brutos.

## Executar localmente

Requisitos: Node.js 22.22.3 ou superior na linha 22, npm e Java 21 para a API. [Compatibilidade do Angular](https://angular.dev/reference/versions).

Em um terminal, na raiz do repositório:

```sh
cd ingresso-api
export TOKEN_SECRET="$(openssl rand -hex 32)"
./mvnw spring-boot:run
```

Em outro terminal:

```sh
cd ingresso-web
npm ci
npm start
```

Abra http://localhost:4200. Crie uma conta e depois entre com o e-mail e a senha cadastrados. Não há credenciais fixas ou dados simulados no front-end. A API persiste localmente em H2 file por Flyway; o arquivo fica em `ingresso-api/data/` e não é versionado. `TOKEN_SECRET` precisa ter pelo menos 64 bytes UTF-8.

`proxy.conf.json` encaminha `/api/**` para `http://localhost:8081`. Se a API usar outra porta ou host, altere o `target` e reinicie o front-end. O cache offline do scanner é ativado no build de produção com service worker, não no `ng serve`.

## Funcionalidades e contratos

| Tela        | Operação                | Contrato da API                                                                                  |
| ----------- | ----------------------- | ------------------------------------------------------------------------------------------------ |
| `/cadastro` | Criar conta comum       | `POST /api/usuarios`, corpo `{ nome, email, password }`, sucesso `201` sem corpo |
| `/entrar`   | Entrar                  | `POST /api/auth/login`, corpo `{ email, password }`, sucesso `200` com `{ token }`               |
| `/conta`    | Consultar sessão e sair | Usa `sub` (e-mail) e `exp` do JWT recebido; sair remove a sessão local                           |
| `/eventos` | Catálogo público | Lista eventos publicados; detalhe exibe categorias, disponibilidade configurada e mapa de assentos |
| `/produtor/eventos` | Gestão do produtor | Cria/publica, edita, lista, reativa e cancela eventos; API valida papel e ownership |
| `/produtor/eventos/:id/financeiro` | Relatório do produtor | Vendas brutas, refunds simulados e bruto restante; limitado ao dono |
| `/admin/eventos` | Moderação | Lista, suspende ou cancela eventos; API exige ADMIN |
| `/admin/financeiro` | Relatório ADMIN | Agrega vendas e refunds simulados por evento; não inclui comissão/repasse |
| `/eventos/:id/comprar` | Checkout autenticado | Seleciona categorias/assentos, cria pedido com `Idempotency-Key` e inicia o mock |
| `/pedidos` | Histórico autenticado | Exibe itens/QR e permite pedir refund simulado por ticket elegível |
| `/eventos/:id/entrada` | Operação de entrada | Produtor dono/equipe/Admin validam QR; operação offline fica provisória até sincronizar |
| `/convites/equipe/aceitar` | Aceitar convite | Operador autenticado aceita grant vinculado ao e-mail para eventos específicos |

O front trata campos obrigatórios, e-mail inválido, confirmação de senha, cadastro duplicado (409), credenciais recusadas (401/403), indisponibilidade e timeout de 15 segundos. O botão de envio fica bloqueado durante a requisição.

O frontend não oferece listagem/edição de usuários, consulta de perfil, recuperação de senha, renovação/revogação de token, pagamento/refund real ou envio real de e-mail. A API oferece `GET /api/usuarios` paginado e restrito a ADMIN, mas o frontend não o consome; também não há endpoint de perfil próprio. Por isso nome e perfil não são inventados na área da conta.

## Responsabilidade, dependências e entradas

Este módulo implementa as jornadas de comprador, produtor e admin na web. A entrada é `src/main.ts`, que inicializa `App` com `appConfig`; `app.routes.ts` registra rotas com carregamento lazy. As páginas usam `AuthService`, `EventApi`, guards e interceptor de `core`, e o layout compartilhado de `shared`.

Dependências de runtime principais: Angular 22.1, `@angular/service-worker`, `@zxing/browser`, `idb`, RxJS 7.8 e tslib. Ferramentas de build/teste: Angular CLI/build, TypeScript 6, Vitest e jsdom. Service worker é ativado no build de produção; o proxy dev não oferece garantia de execução offline.

## Sessão

O token fica em `sessionStorage`, com alternativa em memória se o armazenamento estiver indisponível. Senhas não são armazenadas. A sessão é restaurada ao recarregar a mesma aba e removida ao sair ou expirar. Rotas autenticadas usam um guard; rotas públicas redirecionam sessões válidas para `/conta`.

O interceptor envia `Authorization: Bearer` apenas em requisições privadas para a base da própria API. Login e cadastro não recebem esse cabeçalho. Uma resposta 401 de uma rota privada encerra a sessão. A leitura dos claims no navegador serve à interface: autenticação e autorização efetivas continuam sendo responsabilidade do servidor.

O cadastro público não envia papel. O backend também ignora o campo legado `perfil` se um cliente antigo ainda o enviar e sempre atribui `USER`. Produtores são concedidos por convite ADMIN com token de uso único vinculado ao e-mail e expiração de 48 horas. O primeiro ADMIN é provisionado manualmente conforme o [runbook](../specs/001-foundation-security/manual-admin-provisioning.md). Além disso, `nome` tem restrição de unicidade no banco, mas apenas conflitos de e-mail recebem tratamento específico na API; um nome duplicado pode resultar em erro genérico.

## Organização

- `src/app/core`: contratos de auth/events/orders/entry/finance, IndexedDB para manifestos hash-only e fila offline, guards e interceptor.
- `src/app/pages`: catálogo, checkout/histórico com QR/refund, operação de entrada, relatórios e equipe por evento.
- `src/app/shared`: composição visual compartilhada.
- `src/styles.css`: identidade visual e adaptação para celular.

O pagamento e refunds são mock e não movimentam dinheiro. O histórico permite solicitar refund por ticket elegível e mostra o estado SIMULATED; relatórios não inventam comissão/repasse. O manifesto offline vale 24 horas, contém hashes e fica vinculado ao operador.

## Validar e compilar

```sh
npm test -- --watch=false
npm run build
```

Os testes cobrem auth, catálogo/detalhe, editor/seat map, checkout, histórico/tickets e rotas protegidas; a compilação usa TypeScript e templates estritos.

A saída de produção fica em `dist/ingresso-web/browser`. Configure o servidor web para servir `index.html` nas rotas da aplicação e encaminhar `/api/` para o back-end. O proxy do Angular é exclusivo do desenvolvimento. Para usar uma API em outro domínio, sobrescreva o provider `API_URL` em `app.config.ts` com a URL completa, incluindo `/api`, e configure o CORS no servidor.

A tipografia usa Google Fonts com fontes locais de fallback; a interface permanece utilizável sem acesso ao serviço. Os desenhos dos ingressos são feitos em CSS, sem depender de imagens externas.
