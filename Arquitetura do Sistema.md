# Arquitetura do Sistema

**Escopo analisado:** código e configurações presentes no repositório em 2026-10-06. Este documento descreve o estado observado, não um desenho futuro presumido.

## Visão arquitetural

O sistema é composto por dois projetos executáveis independentes, sem módulo agregador ou dependência de compilação entre eles:

| Módulo | Tecnologia e entrada | Responsabilidade comprovada |
| --- | --- | --- |
| [`ingresso-api`](ingresso-api/README.md) | Java 21, Spring Boot 4.1.1; `IngressoApiApplication` | API HTTP para identidade/segurança, eventos/ofertas, catálogo público e moderação, com persistência JPA/Flyway. |
| [`ingresso-web`](ingresso-web/README.md) | Angular 22; `src/main.ts` | Catálogo/detalhe público, mapa de assentos, editor do produtor, moderação e jornadas de conta; integra os contratos HTTP da API. |

O navegador comunica-se com a API por HTTP sob `/api`. No desenvolvimento, o servidor Angular encaminha esse prefixo para `http://localhost:8081` por `proxy.conf.json`. Em produção, o projeto requer hospedagem do bundle Angular e configuração externa de proxy/reverse proxy; o proxy do Angular não faz parte do build publicado.

Há código de domínio para eventos, categorias/ofertas, pedidos, tickets QR e validação de entrada. Reservas decrementam a disponibilidade configurada enquanto ativas; vendas aprovadas viram estoque comprometido e emitem tickets. Pagamento/e-mail reais e operação financeira permanecem fora do escopo local.

## Separação de responsabilidades e dependências

| Área | Responsabilidade e dependências internas |
| --- | --- |
| API `controller` | Adaptar HTTP para serviços; depende de DTOs, serviços e configuração de autorização. |
| API `service` | Regras de usuário/convite, eventos/ofertas e pedidos; bloqueia inventário concorrente, reserva/expira estoque, processa mock e snapshots de preço. |
| API `repository` | Acesso JPA a usuários, eventos, convites, categorias, assentos, pedidos e pagamentos; locks pessimistas protegem a reserva. |
| API `model` / `dto` | Persistência e contratos HTTP para papéis, eventos/ofertas/mapa, pedido, itens, reservas e tentativas de pagamento. |
| API `security` / `config` | Carregar usuário, gerar/verificar JWT, filtrar requisições, configurar regras HTTP, CORS, OpenAPI e tratamento de exceções. |
| Web `core` | URL e tipos de API, cliente de autenticação, armazenamento/expiração de sessão, interceptor, guards e validação. |
| Web `pages` | Telas lazy-loaded de conta/cadastro, catálogo/detalhe, checkout, histórico e gestão de eventos; dependem de `core`. |
| Web `shared` | Layout visual comum às telas de autenticação; não possui acesso HTTP próprio. |

Não há chamada de módulo Angular diretamente ao banco. O frontend depende dos caminhos, payloads e semântica HTTP implementados pelos controllers. A API não depende do frontend e pode receber clientes HTTP distintos.

## Fluxos entre módulos

1. **Cadastro:** formulário valida nome, e-mail e confirmação da senha; envia `POST /api/usuarios`. A API ignora o campo legado `perfil`, atribui somente `USER`, codifica a senha com BCrypt e persiste `Usuario`. Resposta de sucesso é `201` sem corpo.
2. **Login:** o web envia `POST /api/auth/login`; a API autentica com `AuthenticationManager`, consulta `UsuarioRepository`, compara senha codificada e devolve `{ token }`. O JWT tem emissor `ingresso-api`, subject igual ao e-mail e expiração configurada para duas horas.
3. **Sessão web:** o Angular lê `sub`, `iss` e `exp` para exibir e encerrar a sessão; armazena o token em `sessionStorage` (ou mantém em memória se armazenamento falhar). Essa leitura não valida a assinatura e não substitui a validação do servidor.
4. **Requisição autenticada:** o interceptor envia `Authorization: Bearer` apenas à API e em chamadas que não sejam login/cadastro. `SecurityFilter` valida o JWT e recarrega o usuário e seus papéis do banco em cada requisição; alterações de papel passam a valer sem aguardar a expiração do token.
5. **Listagem administrativa:** `GET /api/usuarios` recebe `Pageable`, exige `ROLE_ADMIN` e responde com `Page<UsuarioResponse>`. O frontend atual não consome essa rota.
6. **Convite de produtor:** ADMIN cria convite vinculado ao e-mail, recebe o token uma única vez e o transmite fora de banda. O token de 256 bits é armazenado como SHA-256 e expira em 48 horas; o usuário autenticado com o mesmo e-mail aceita uma vez. Criação, aceite, revogação e alteração de papel geram registros de auditoria.
7. **Eventos e ofertas:** produtor autenticado cria/publica e edita somente os próprios eventos; comprador consulta lista/detalhe público e oferta; cada assento herda categoria/preço. ADMIN suspende/cancela e o produtor dono pode reativar um suspenso.
8. **Pedido, pagamento mock e entrega:** comprador autenticado envia evento/itens com `Idempotency-Key`; o backend reserva unidades/assentos até 15 minutos ou `startsAt`. Após aprovação, emite um QR opaco por unidade e solicita confirmação ao adapter de e-mail mock em `dev/test`; o dono consulta os tickets no histórico. Scanner e provedores reais permanecem futuros.
9. **Validação de entrada:** produtor dono, equipe aceita para o evento e ADMIN validam no dia local do evento publicado. Online o servidor consome o ticket uma vez; offline o navegador usa manifesto de 24 horas com hashes, mantém scans provisórios e sincroniza com conflitos resolvidos pelo primeiro sync. Correções ADMIN são auditadas e reabrem um ticket uma vez.
10. **Pós-venda mock:** comprador solicita refund automático por ticket pago/não usado antes do início; cancelamento de evento cria refunds simulados para tickets pagos/não usados. Relatórios producer/Admin reconciliam bruto/refunds; comissão/repasse não são calculados sem política definida.
10. **Pós-venda mock:** comprador solicita refund automático por ticket pago/não usado antes do início; cancelar evento cria refunds mock para todos os tickets pagos/não usados. Relatórios de produtor dono e ADMIN reconciliam valor bruto e simulado; comissão/repasse não são calculados sem política confirmada.

## Padrões observados

- Organização backend em camadas controller → service → repository, com entidade JPA, interfaces de serviço, DTOs record e injeção de dependências por construtor.
- Spring Security stateless, filtro JWT antes de `UsernamePasswordAuthenticationFilter`, `@PreAuthorize`, BCrypt e validação Jakarta Bean Validation.
- Papéis de conta acumuláveis; cadastro público atribui apenas USER; convites administrativos são tokens aleatórios de uso único armazenados em hash.
- Flyway aplica migrations versionadas; Hibernate valida o schema sem modificá-lo. H2 em arquivo é o datasource configurável atual.
- Compra usa snapshots de preço/categoria/assento, QR opaco individual, chave idempotente por comprador, locks pessimistas de inventário, gateway `PaymentGateway` e adapter `EmailGateway`; mocks não cobram nem enviam e-mail externo.
- Pós-venda usa `RefundGateway` mock, status `SIMULATED`, unicidade por ticket e preço snapshot; relatórios não representam liquidação, comissão ou payout.
- Pós-venda usa `RefundGateway` em perfis mock, estado `SIMULATED`, preço snapshot e refund único por ticket; relatórios não representam liquidação financeira.
- API REST, paginação Spring Data, OpenAPI configurado para Bearer JWT e handler central para `IllegalArgumentException`.
- Angular standalone, providers funcionais, rotas lazy-loaded, guards, interceptor funcional, formulários reativos e estado de sessão com signals.
- Testes backend de contexto, token, cadastro seguro, convites, migração, eventos/mapa, checkout concorrente, idempotência, tickets, check-in concorrente, grants, correções, sync offline, refunds e relatórios; testes web de auth, catálogo, editor/seat map, checkout, histórico, scanner e relatórios.

Estes são padrões observados no código, não garantias de que todo novo módulo já os siga.

## Convenções técnicas

- Backend: pacote base `org.example.ingresso.ingresso`; tipos Java em PascalCase, propriedades/métodos em camelCase; DTOs em `dto`, endpoints em `controller` e implementações concretas no subpacote `service.impl`.
- Web: funcionalidade agrupada em `src/app/core`, `src/app/pages` e `src/app/shared`; nomes de arquivo em kebab-case; contratos e API base centralizados em `core/api.ts`.
- A base da API no web é `/api`; não espalhar URLs de serviço por componentes. O proxy local aponta para a porta `8081`.
- Mensagens de interface e algumas mensagens da API estão em português. Manter consistência com o idioma e com os contratos já expostos.
- Nenhum arquivo de configuração de produção para banco, segredo, CORS, observabilidade ou implantação foi encontrado na configuração analisada; não assumir que os valores locais sejam apropriados para produção.

## Dependências críticas

- API: Java 21; Spring Boot 4.1.1; starters Web MVC, Security, Validation e Data JPA; H2 em runtime; `java-jwt` 4.4.0; Springdoc OpenAPI 2.8.13. Versões e escopos estão em `ingresso-api/pom.xml`.
- Web: Angular 22.1, TypeScript 6, RxJS 7.8; scripts npm e versão do gerenciador em `ingresso-web/package.json`.
- Execução integrada local depende da API em `8081` e do proxy Angular; uso em outro host depende de provider `API_URL` e CORS configurado no servidor.
- H2 em arquivo, Flyway e `TOKEN_SECRET` obrigatório vêm de `ingresso-api/src/main/resources/application.properties`.

## Riscos, acoplamentos e lacunas

- **Persistência local:** H2 em arquivo mantém dados entre reinícios, mas não oferece por si só configuração de alta disponibilidade/multi-instância para produção.
- **Admin inicial:** o primeiro ADMIN depende de operação manual no banco; proteger o acesso ao console H2 e seguir o [runbook](specs/001-foundation-security/manual-admin-provisioning.md).
- **Entrega de convite:** o token de produtor é exposto uma vez na resposta para o administrador; não há integração de e-mail para convites.
- **Escopo pendente:** mock não movimenta dinheiro e pagamento real, reembolso, taxa, QR/email, staff/scanner são features posteriores. Quantidades vendidas ficam comprometidas; pedidos pagos não geram ticket nesta fase.
- **Políticas em aberto:** limites máximos por categoria, vocabulário de classificação, exibição de evento passado/suspenso, edição ADMIN e metadados de acessibilidade.
- **Paginação:** o service cria `PageImpl` com `usuarios.size()` como total, que é o tamanho da página carregada, não o total retornado pelo repositório. Metadados de páginas podem ficar incorretos.
- **Contrato e persistência:** a entidade declara `nome` único; a verificação explícita do service cobre somente e-mail. Falhas de integridade de nome não têm tratamento específico no handler observado.
- **Autorização parcialmente exercida:** ownership de produtor está aplicado nos eventos; concessão de acesso à equipe de entrada por escopo de evento ainda depende da feature do scanner. `Constants.SECURITY_ROLE_USER` não tem uso encontrado.
- **Acoplamento de integração:** nomes de rotas, formato `{ token }`, campos `email/password/perfil` e claims `sub/iss/exp` precisam permanecer compatíveis entre API e web. Não há contrato gerado compartilhado.
- **Escopo de domínio incompleto:** não existem módulos de evento ou ingresso no código examinado. Não tratar interfaces decorativas como evidência de compra.
- **Migração legado:** grants `ADMIN` da tabela antiga são rebaixados a `USER`, pois sua origem não é confiável; administradores legítimos precisam ser revisados e reprovisionados manualmente.
- **Pastas sem implementação:** `src/main/resources/static` e `templates` aparecem na estrutura, mas não contêm arquivos informados. Não são módulos ativos comprovados. Não há evidência de que precisem ser removidas.

## Regras e diretrizes para novas implementações

**Regras implementadas:** autorização é validada no servidor; claims decodificados no browser não são fonte de autorização; cadastro público cria apenas USER; grants ADMIN são manuais; convite de produtor é vinculado ao e-mail, expira em 48 horas e não pode ser reutilizado; senha/token nunca são persistidos em claro; migrations são a autoridade do schema.

**Diretrizes recomendadas (não implementadas automaticamente):**

1. Antes de adicionar domínio, documentar ator, invariantes, estados, erros e contrato HTTP em uma especificação; vincular a especificação ao teste que a verifica.
2. Manter controllers focados em HTTP, regras em services, persistência em repositories e fronteiras de dados em DTOs; não expor entidades JPA diretamente.
3. Não reintroduzir atribuição de papel privilegiado por payload público; atualizar/remove o campo legado `perfil` quando houver versão coordenada do contrato web/API.
4. Manter segredo e configuração por ambiente; planejar banco de produção antes de publicar, pois H2 em arquivo atende somente o escopo local/MVP atual.
5. Preservar compatibilidade API-web mediante testes de contrato; cobrir autorização, casos de erro, paginação e persistência no backend.
6. Acrescentar novos módulos de produto apenas com entidades, serviços, endpoints, testes e documentação que demonstrem o comportamento, sem inferi-lo da marca.
7. Atualizar este documento e os READMEs locais na mesma mudança quando responsabilidades, dependências, fluxos ou contratos mudarem.

As recomendações acima são diretrizes de manutenção, não descrição de capacidades já existentes.