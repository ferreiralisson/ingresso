# Objetivo do Sistema

**Base factual:** análise do código e das configurações presentes em 2026-10-06. Inferências estão explicitamente marcadas como **Hipótese**.

## Propósito confirmado

O sistema disponibiliza uma interface web e uma API para contas, eventos/ofertas, pedidos com pagamento mock, tickets QR, validação de entrada e pós-venda com refunds simulados e relatórios brutos.

## Problemas que resolve no escopo implementado

- Permite criar conta de comprador; o perfil enviado pelo cliente não concede privilégios.
- Permite autenticar credenciais e emitir um token JWT.
- Restringe chamadas HTTP privadas a tokens válidos, acumula papéis, limita gestão de produtores a ADMIN e limita pedidos/refunds/histórico ao comprador dono.
- Permite que ADMIN convide produtores por e-mail, com aceite autenticado, token de uso único de 48 horas e auditoria de concessão/revogação.
- Permite que produtores criem/publiquem/editem/cancelem seus eventos, gerenciem equipe event-scoped e consultem relatórios brutos dos próprios eventos; compradores consultam catálogo/ofertas.
- Permite moderação ADMIN para suspender/cancelar e consultar relatórios financeiros agregados.
- Permite ao comprador criar um pedido de um evento, consultar seu histórico e iniciar pagamento mock aprovado/recusado/pendente.
- Reserva categorias/assentos por até 15 minutos ou até o início do evento, com controle concorrente e idempotência por chave.
- Oferece telas para iniciar sessão, descobrir eventos, checkout/histórico com QR, operação online/offline e solicitação de refund por ticket elegível.

O checkout persiste pedidos e simula pagamento sem mover dinheiro. Refunds também são simulados; os relatórios conciliam valores brutos e refunds sem calcular comissão/repasse. Nenhum dinheiro real é movimentado.

## Atores

| Ator | Interação observada |
| --- | --- |
| Visitante | Pode cadastrar usuário e tentar login pelo web ou chamar os endpoints públicos da API. |
| Comprador autenticado (`USER`) | Pode criar pedidos, solicitar refund de tickets próprios elegíveis e consultar o próprio histórico; cobrança/refund são mock. |
| Administrador (`ADMIN`) | Pode acessar `GET /api/usuarios`, moderar eventos, corrigir check-ins e consultar relatórios agregados. O primeiro ADMIN é provisionado manualmente no banco. |
| Produtor (`PRODUCER`) | Pode gerir/cancelar os próprios eventos, convidar equipe e consultar seus relatórios de valores brutos. |
| Equipe de entrada | Conta autenticada com grant aceito para eventos selecionados; pode validar somente os tickets desses eventos. |
| Visitante/comprador | Consulta agenda/detalhe público; usuário autenticado seleciona ofertas, cria pedidos e consulta somente seu próprio histórico. |
| Cliente HTTP | Pode consumir a API diretamente; não precisa ser o Angular. |

## Fluxos de negócio presentes

1. Cadastro público cria usuário, armazena senha codificada com BCrypt e devolve `201` sem corpo.
2. Login verifica e-mail/senha e retorna um JWT com validade de duas horas.
3. O web mantém o token na aba, expira a sessão local conforme `exp`, protege a rota `/conta`, encerra sessão ao sair e reage a `401` em chamada privada.
4. Usuário ADMIN pode consultar a listagem paginada de usuários pela API; não existe tela Angular para esse fluxo.
5. Comprador cria pedido de um evento, reserva categorias/assentos, inicia resultado mock e acompanha status no histórico.
6. Operador valida QR por câmera ou código manual; no modo offline os aceites são provisórios até a sincronização e resolução de conflitos.
7. Comprador solicita refund simulado por ticket elegível; cancelamento do evento também gera refunds simulados para tickets pagos não usados; relatórios reconciliam vendas e refunds.

## Funcionalidades centrais e limites

- API: contas/auth, catálogo/eventos, convites, pedidos/tickets, check-in, refund mock por ticket e relatórios bruto/refundado com ownership.
- API: criar/aceitar/revogar convite de produtor e revogar papel de produtor; consulte o README da API para os caminhos HTTP.
- API/web: catálogo público, gestão de eventos/ofertas, checkout de evento único, pagamento mock, reservas e histórico do comprador.
- Web: cadastro/conta, catálogo/checkout/histórico, operação de entrada, aceite de equipe, reports producer/Admin e solicitação de refund.
- Operação local: API na porta `8081`, frontend servido pelo Angular em desenvolvimento com proxy para `/api`; banco H2 em arquivo com migrations Flyway; `TOKEN_SECRET` precisa ser fornecido e ter pelo menos 64 bytes.
- Fora do escopo comprovado: movimento real de pagamento/refund, taxa, comissão, net payable, payout e provedor real. Refunds e e-mail são somente mock.

## Visão de produto

**Hipótese:** o nome `ingresso`, textos da interface e ilustrações sugerem intenção de evoluir para uma plataforma relacionada a experiências ou ingressos. O código não permite determinar público-alvo, modelo comercial, regras de venda, catálogo ou roadmap. A visão de produto deve ser confirmada pelos responsáveis antes de orientar especificações novas.

## Contexto operacional

Os repositórios dos módulos estão lado a lado e cada um possui seu próprio manifesto e ciclo de build. O frontend chama a API pelo prefixo `/api`; o proxy de desenvolvimento aponta para `localhost:8081`. O token é validado na API, enquanto o navegador decodifica claims apenas para controlar a experiência de sessão. H2 em arquivo e segredo externo obrigatório melhoram a operação local, mas não constituem configuração de produção. Não foram encontrados arquivos de implantação, monitoramento ou configuração de ambiente produtivo no escopo analisado.

Para documentação de arquitetura, dependências, riscos e diretrizes, consulte [Arquitetura do Sistema](Arquitetura%20do%20Sistema.md), [ingresso-api](ingresso-api/README.md) e [ingresso-web](ingresso-web/README.md).