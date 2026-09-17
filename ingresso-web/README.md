# ingresso-web

Front-end Angular de integração com `../ingresso-api`. Interface em português, responsiva, com componentes standalone, carregamento de páginas sob demanda, formulários reativos e autenticação JWT.

## Executar localmente

Requisitos: Node.js 22.22.3 ou superior na linha 22, npm e Java 21 para a API. [Compatibilidade do Angular](https://angular.dev/reference/versions).

Em um terminal, na raiz do repositório:

```sh
cd ingresso-api
./mvnw spring-boot:run
```

Em outro terminal:

```sh
cd ingresso-web
npm ci
npm start
```

Abra http://localhost:4200. Crie uma conta e depois entre com o e-mail e a senha cadastrados. Não há credenciais fixas ou dados simulados no front-end. O banco H2 configurado no back-end é em memória: os cadastros são perdidos quando a API reinicia.

`proxy.conf.json` encaminha `/api/**` para `http://localhost:8081`. Se a API usar outra porta ou host, altere o `target` e reinicie o front-end.

## Funcionalidades e contratos

| Tela        | Operação                | Contrato da API                                                                                  |
| ----------- | ----------------------- | ------------------------------------------------------------------------------------------------ |
| `/cadastro` | Criar conta comum       | `POST /api/usuarios`, corpo `{ nome, email, password, perfil: "USER" }`, sucesso `201` sem corpo |
| `/entrar`   | Entrar                  | `POST /api/auth/login`, corpo `{ email, password }`, sucesso `200` com `{ token }`               |
| `/conta`    | Consultar sessão e sair | Usa `sub` (e-mail) e `exp` do JWT recebido; sair remove a sessão local                           |

O front trata campos obrigatórios, e-mail inválido, confirmação de senha, cadastro duplicado (409), credenciais recusadas (401/403), indisponibilidade e timeout de 15 segundos. O botão de envio fica bloqueado durante a requisição.

A API atual não oferece listagem/edição de usuários, consulta de perfil, recuperação de senha, renovação/revogação de token, eventos ou compra de ingressos. Não existem chamadas a endpoints fictícios. `UsuarioResponse` existe no back-end, mas não há rota que o retorne; por isso nome e perfil não são inventados na área da conta.

## Sessão

O token fica em `sessionStorage`, com alternativa em memória se o armazenamento estiver indisponível. Senhas não são armazenadas. A sessão é restaurada ao recarregar a mesma aba e removida ao sair ou expirar. Rotas autenticadas usam um guard; rotas públicas redirecionam sessões válidas para `/conta`.

O interceptor envia `Authorization: Bearer` apenas em requisições privadas para a base da própria API. Login e cadastro não recebem esse cabeçalho. Uma resposta 401 de uma rota privada encerra a sessão. A leitura dos claims no navegador serve à interface: autenticação e autorização efetivas continuam sendo responsabilidade do servidor.

O cadastro público envia sempre `USER`. O back-end também aceita `ADMIN` no endpoint público; ocultar essa opção na interface não impõe autorização ao servidor. Além disso, `nome` tem restrição de unicidade no banco, mas apenas conflitos de e-mail recebem tratamento específico na API; um nome duplicado pode resultar em erro genérico. Essas regras não foram alteradas no back-end.

## Organização

- `src/app/core`: contratos tipados, endereço da API, serviço de autenticação, guards, interceptor e validações.
- `src/app/pages`: login, cadastro e conta.
- `src/app/shared`: composição visual compartilhada.
- `src/styles.css`: identidade visual e adaptação para celular.

## Validar e compilar

```sh
npm test -- --watch=false
npm run build
```

Os testes cobrem contratos HTTP, resposta 201 vazia, erros, proteção de rotas, restauração e expiração da sessão, isolamento do Bearer e fluxos de formulário. A compilação usa TypeScript e templates estritos.

A saída de produção fica em `dist/ingresso-web/browser`. Configure o servidor web para servir `index.html` nas rotas da aplicação e encaminhar `/api/` para o back-end. O proxy do Angular é exclusivo do desenvolvimento. Para usar uma API em outro domínio, sobrescreva o provider `API_URL` em `app.config.ts` com a URL completa, incluindo `/api`, e configure o CORS no servidor.

A tipografia usa Google Fonts com fontes locais de fallback; a interface permanece utilizável sem acesso ao serviço. Os desenhos dos ingressos são feitos em CSS, sem depender de imagens externas.
