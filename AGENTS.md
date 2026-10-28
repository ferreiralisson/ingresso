# Instrucoes para agentes

## Contexto do repositorio

O sistema tem dois projetos executaveis independentes, sem modulo agregador:

- `ingresso-api/`: API REST Java 25 com Spring Boot 4.1.1, JPA, Flyway e Spring Security.
- `ingresso-web/`: aplicacao Angular 22 standalone, TypeScript 6 e RxJS.
- `docs/Arquitetura do Sistema.md`: arquitetura observada, fluxos, riscos e convencoes.
- `docs/specs/`: especificacoes funcionais que definem os comportamentos esperados.

Leia a arquitetura e a especificacao relevante em `docs/` antes de mudar contratos ou regras de dominio. A arquitetura descreve o estado observado; nao trate funcionalidades futuras ou mock como capacidades de producao.

## Comandos de desenvolvimento

Execute os comandos dentro do respectivo modulo:

```sh
# API
cd ingresso-api
export TOKEN_SECRET="$(openssl rand -hex 32)"
./mvnw test
./mvnw spring-boot:run

# Frontend
cd ingresso-web
npm ci
npm test -- --watch=false
npm run build
npm start
```

A API inicia por padrao na porta `8081`; o servidor Angular inicia na `4200` e o proxy local encaminha `/api/**` para a API. `TOKEN_SECRET` deve ter pelo menos 64 bytes UTF-8. O H2 em arquivo e o proxy do Angular sao configuracoes locais; nao os considere configuracao de producao. O console H2 e opt-in e deve permanecer restrito a ambiente local confiavel.

## Diretrizes gerais

- Mantenha alteracoes focadas e alinhadas as especificacoes, aos contratos existentes e a arquitetura documentada.
- Preserve compatibilidade entre API e web. Os contratos HTTP sao definidos nos DTOs/controllers da API e nos modelos/clientes de `ingresso-web/src/app/features/<feature>`; nao espalhe URLs HTTP em componentes.
- Mantenha mensagens e textos de interface em portugues, seguindo o idioma existente.
- Atualize documentacao da camada e `docs/Arquitetura do Sistema.md` quando uma mudanca alterar responsabilidades, fluxos, contratos ou restricoes documentadas.
- Nao adicione funcionalidades presumidas a partir do objetivo do produto. Pagamentos, reembolsos, entrega de e-mail e relatorios financeiros atuais sao simulados; nao movimentam dinheiro nem calculam comissao ou repasse.

## API

- Siga a separacao `controller` -> `service` -> `repository`; controllers adaptam HTTP, services concentram regras e repositories acessam persistencia.
- Use DTOs para fronteiras HTTP; nao exponha entidades JPA diretamente. Mantenha validacao de entrada com Jakarta Bean Validation e injecao por construtor conforme os padroes existentes.
- Services publicos sao interfaces em `service`; implementacoes ficam em `service.impl` e contratos de aplicacao em `service.dto`. Controllers e colaboracoes entre services dependem das interfaces. Contratos nao devem expor entidades JPA, tipos HTTP/Spring ou principals de seguranca.
- Mantenha as regras de `ServiceBoundaryTest` (ArchUnit): controllers nao importam `service.impl`, DTOs/interfaces nao dependem de models/repositories/frameworks, e implementacoes nao chamam implementacoes concretas.
- A autorizacao efetiva pertence ao servidor. Verifique papel, identidade e ownership nas regras existentes; claims lidos pelo navegador nunca autorizam operacoes.
- Cadastro publico sempre cria `USER`; nunca aceite papel privilegiado vindo do cliente. Convites e grants mantem escopo, vinculo de identidade, expiracao e uso unico descritos pelas especificacoes.
- `src/main/resources/db/migration` e a autoridade para alteracoes de schema. Adicione uma nova migration Flyway em vez de depender de alteracoes automaticas do Hibernate, que esta configurado para validar o schema.
- Preserve transacoes, locks e idempotencia nos fluxos concorrentes de pedidos, inventario, tickets e entrada. QR, tokens de convite e manifestos offline devem continuar protegidos conforme o fluxo existente.
- Nunca registre ou persista senhas, segredos, tokens de convite ou QR em texto claro sem uma necessidade explicitamente definida. Nao introduza segredo padrao ou credenciais de desenvolvimento no codigo.
- Execute `./mvnw test` apos mudancas na API; acrescente ou ajuste testes para contratos, autorizacao, erros e persistencia afetados.

## Frontend

- Organize o web por funcionalidade em `src/app/features/<feature>`; mantenha infraestrutura transversal em `src/app/core` e componentes visuais sem dominio em `src/app/shared`.
- Siga o padrao standalone, providers funcionais, rotas lazy-loaded, formularios reativos e signals quando compativel com a implementacao existente.
- Mantenha `API_URL` em `core/http` e sessao, interceptor e guards genericos em `core/auth`; cada feature mantem seus clientes HTTP em `data-access` e contratos em `models`. Preserve os limites de autorizacao: guards nao substituem o servidor, e o interceptor envia Bearer apenas para chamadas privadas a API.
- Evite dependência de `core` em uma feature; guards de navegação que dependem de estado específico, como manifesto offline de entrada, pertencem à feature.
- Trate a sessao local e os claims JWT como apoio de interface, nunca como fonte de autorizacao. Nao armazene senhas.
- Para checkout, mantenha `Idempotency-Key` e nao permita que o cliente escolha o resultado do pagamento. Para entrada offline, manifeste hashes e scans provisórios; o servidor continua autoridade para validar e sincronizar.
- Execute `npm test -- --watch=false` e `npm run build` apos mudancas no web. Cubra estados de carregamento/erro e os fluxos afetados nos testes existentes.

## Integracao e validacao

- API e web sao implantados independentemente. O proxy em `ingresso-web/proxy.conf.json` e somente para desenvolvimento; producao precisa servir o bundle Angular e encaminhar `/api/` ao backend.
- Mudancas de endpoint ou payload devem ser coordenadas nas duas camadas e verificadas com testes dos dois modulos quando afetarem ambos.
- Em mudancas de banco, seguranca, pedidos ou validacao offline, consulte as especificacoes em `docs/specs/` e valide os casos de concorrencia/autorizacao relevantes, nao apenas a compilacao.