# Funcionalidades a Implementar

**Status:** backlog de evolução baseado na entrevista de 2026-10-06. Fundação e segurança, Eventos e ofertas e Pedido e pagamento mock estão implementadas e especificadas em [001](specs/001-foundation-security/spec.md), [002](specs/002-events-and-offers/spec.md) e [003](specs/003-order-and-mock-payment/spec.md). As fases seguintes continuam planejadas; requisitos sem resposta permanecem pendências.

## 1. Direção do produto

Construir um marketplace web responsivo de venda de ingressos no Brasil. A primeira entrega deve demonstrar uma fatia vertical ponta a ponta: produtor cria e publica um evento, comprador autenticado seleciona ingresso, conclui o fluxo com pagamento simulado, recebe ingresso digital com QR e o código pode ser validado na entrada.

### Decisões confirmadas na entrevista

- Operação marketplace com produtores terceiros; escopo inicial Brasil e web responsiva.
- Atores: comprador, produtor e operação/admin. Também haverá equipe convidada para validar entradas.
- Produtor só obtém perfil por convite administrativo. Depois de autorizado, pode publicar eventos diretamente, sem aprovação prévia.
- Vendas poderão usar ingressos sem assento marcado e ingressos com assento marcado.
- Um pedido contém ingressos de um único evento.
- Comprador precisa estar autenticado para comprar.
- Estoque não pode ser excedido, inclusive em tentativas concorrentes.
- Pagamento deve começar simulado, configurável para resultado aprovado, recusado ou pendente, e ter estrutura substituível por integração real.
- Após a compra, o comprador deve ter pedido/histórico na conta, receber ingresso digital com QR único e confirmação por e-mail.
- E-mail começa com implementação simulada substituível por provedor real.
- Comprador pode solicitar cancelamento/reembolso. A entrevista não definiu a política de elegibilidade nem a execução financeira do reembolso.
- A plataforma cobra comissão percentual. Percentual, cálculo, retenção e repasse ainda não foram definidos.
- Scanner web deve permitir validação pelo produtor dono do evento, equipe convidada por ele e admin.

## 2. Estado atual relevante

O projeto atual tem cadastro/autenticação, convites/papéis, eventos/ofertas/mapas, catálogo, pedidos, reservas de estoque e pagamento mock. QR/ingressos digitais, checkout com provedor real, e-mail e scanner ainda não existem. A API usa Spring Boot/JPA, H2 em arquivo e Flyway; o web usa Angular e chama a API sob `/api`. Ver [Arquitetura do Sistema](Arquitetura%20do%20Sistema.md) e os READMEs dos módulos [API](ingresso-api/README.md) e [web](ingresso-web/README.md).

Antes desta implementação, o cadastro público aceitava `perfil=ADMIN`. Agora o campo legado é ignorado, contas públicas recebem apenas `USER`, e grants `ADMIN` antigos são rebaixados e auditados pela migration. O primeiro administrador é criado pelo procedimento manual documentado.

## 3. Funcionalidades do primeiro escopo

### 3.1 Identidade, papéis e convites

- Manter compradores como usuários autenticados.
- Permitir que admin convide/promova um usuário para produtor; somente produtores autorizados podem criar/publicar seus eventos.
- Permitir convite de equipe de entrada com permissão limitada ao evento ou eventos autorizados pelo produtor.
- Admin mantém acesso operacional global; comprador não recebe permissões de produtor ou admin por dados enviados pelo cliente.
- Definir autorização no servidor para cada operação. Guards Angular servem à navegação e não substituem autorização da API.

**Critérios de aceite:** cadastro público nunca cria ADMIN/produtor; convite inválido ou expirado não concede papel; produtor só altera seus eventos; equipe convidada só valida eventos autorizados; admin consegue administrar globalmente.

### 3.2 Cadastro e publicação de eventos (implementada)

Produtor autorizado deve cadastrar evento com os campos que a entrevista considerou necessários:

- título, descrição e imagem;
- data, horário e local;
- classificação indicativa e organizador;
- ofertas de ingresso, com preço e quantidade;
- mapa de assentos quando houver lugares marcados.

Uma vez cadastrado, o produtor pode publicar diretamente. O evento publicado deve poder ser apresentado ao comprador e associado ao produtor responsável. Campos obrigatórios, formatos, fuso horário, edição depois da venda e ações de suspensão/cancelamento administrativo ainda precisam ser especificados.

**Hipótese funcional para completar o marketplace:** compradores precisarão encontrar eventos em uma listagem e abrir uma página de detalhes antes de escolher ingressos. A entrevista confirmou marketplace e compra, mas não definiu busca, filtros, ordenação nem layout; validar esses requisitos antes de detalhar a interface.

### 3.3 Ofertas, lotes e assentos (categorias e mapa base implementados)

- Permitir ao produtor criar opções de ingresso com preço e quantidade.
- Oferecer modalidade sem assento marcado e modalidade com assento marcado, segundo configuração do evento.
- Para evento com assentos, representar lugares e disponibilidade individual para seleção pelo comprador.
- Impedir que o mesmo lugar seja confirmado em pedidos concorrentes.
- Apresentar preço e disponibilidade antes de iniciar pagamento.

Regras ainda pendentes: múltiplos lotes por tipo, abertura/virada automática de lote, limites de quantidade por comprador, taxa de serviço, descontos/códigos promocionais, cortesias e regras de troca de assento.

### 3.4 Pedido, pagamento e estoque (implementados com mock)

- Exigir usuário autenticado para checkout.
- Limitar cada pedido a um único evento; permitir mais de um ingresso no pedido conforme disponibilidade.
- Criar pedido com itens rastreáveis, valor total discriminado e estado consultável pelo comprador.
- Definir uma fronteira de pagamento para permitir substituir o mock por integração real sem acoplar controllers e regras do pedido ao provedor.
- Implementação mock deve permitir simular pagamento aprovado, recusado e pendente. Ela não deve ser confundida com uma cobrança real.
- Em produção, processar confirmações assíncronas/eventos repetidos de forma idempotente.
- Garantir atomicamente que ingressos/assentos confirmados nunca excedam o estoque disponível.

**Regras implementadas:** reservar por 15 minutos ou até `startsAt`, o que ocorrer primeiro; suspensão/cancelamento encerra pedidos pendentes e libera estoque; aprovação atrasada não confirma pedido; retry exige a mesma `Idempotency-Key` e payload.

**Critérios de aceite:** duas compras concorrentes pelo último ingresso não podem resultar em duas confirmações; pagamentos recusados não geram ingresso válido; pagamento pendente não aparece como pago; repetição de confirmação não duplica pedido, cobrança, comissão ou QR.

### 3.5 Emissão e consulta de ingressos (implementada)

- Após pagamento aprovado e estoque confirmado, emitir ingresso digital com identificador/QR único.
- Exibir pedidos e ingressos na conta do comprador e enviar e-mail de confirmação.
- QR precisa permitir verificar validade e estado de uso no servidor; não confiar apenas no conteúdo lido pelo navegador.
- Implementar envio de e-mail por interface/adapter, com implementação mock no ambiente de desenvolvimento e implementação real substituível posteriormente.

**Critérios de aceite:** pedido não pago não produz ingresso utilizável; ingresso pertence ao pedido e evento corretos; QR inválido ou já utilizado é recusado na validação; estado do pedido e ingresso é consistente entre conta, API e e-mail.

### 3.6 Validação de entrada (implementada)

- Disponibilizar fluxo web de leitura/consulta de QR para produtor do evento, equipe convidada e admin.
- Validar no backend que o operador tem permissão sobre o evento.
- Na primeira validação bem-sucedida, marcar ingresso como utilizado; tentativas posteriores devem sinalizar uso anterior sem aceitar nova entrada.
- Registrar evento, ingresso, operador e horário da validação para auditoria operacional.

Leitura por câmera, funcionamento offline, sincronização posterior e dispositivos compatíveis não foram definidos; validar antes de escolher a implementação de scanner.

### 3.7 Cancelamento, reembolso e comissão (refund mock/relatório bruto implementados; política financeira pendente)

- Permitir ao comprador pedir aprovação automática de refund por ticket pago e não usado antes do início local do evento.
- Ao cancelar evento como produtor dono ou ADMIN, criar refund mock de cada ticket pago e não usado; manter origem, ator, amount snapshot e timestamp auditáveis.
- Oferecer ao produtor relatório bruto apenas dos próprios eventos e ao ADMIN relatório agregado de vendas, refunds simulados e valor bruto restante.
- Não calcular comissão, net payable ou repasse até confirmação de base, taxa e calendário comercial.

**Pendências de negócio:** definir o tratamento do ticket já usado em cancelamento, base/taxa da comissão, taxas de serviço/provedor e sua refundabilidade, agenda de repasse e integração real. O fluxo atual marca refunds como simulados e não movimenta dinheiro.

## 4. Sequência recomendada de implementação

Sequência proposta para entregar e validar a fatia vertical sem espalhar regras financeiras não definidas:

1. **Fundação e segurança (implementada):** papéis, convites, autorização no servidor, persistência local por ambiente e migrações; cadastro público sem autoelevação. Consulte a [especificação](specs/001-foundation-security/spec.md) e o [runbook do primeiro ADMIN](specs/001-foundation-security/manual-admin-provisioning.md).
2. **Eventos e ofertas (implementada):** produtor cadastra/publica evento, configura categorias, preço, quantidade e mapa de assentos; comprador encontra eventos e consulta disponibilidade. Consulte a [especificação](specs/002-events-and-offers/spec.md).
3. **Pedido e pagamento mock (implementados):** checkout autenticado de um evento, reserva/limite por categoria, estados do mock, `Idempotency-Key`, histórico e controle concorrente. Consulte a [especificação](specs/003-order-and-mock-payment/spec.md).
4. **Entrega (implementada):** emitir um ticket por unidade após confirmação, exibir QR no histórico autenticado e encaminhar confirmação com QR ao adapter de e-mail mockável. Consulte a [especificação](specs/004-ticket-delivery/spec.md).
5. **Operação no evento (implementada):** scanner por câmera/manual, autorização event-scoped, check-in único, uso offline provisório com sincronização first-sync-wins e auditoria. Consulte a [especificação](specs/005-entry-validation/spec.md).
6. **Pós-venda e financeiro (mock implementado):** solicitação automática por ticket elegível, refund simulado em cancelamento do evento e relatório de valores brutos/reembolsados. Comissão, repasse e integração real permanecem pendentes. Consulte a [especificação](specs/006-post-sale-and-finance/spec.md).

Esta sequência é uma recomendação de entrega, não uma decisão sobre provedor, modelo financeiro ou política legal.

## 5. Decisões necessárias antes de detalhar histórias

1. Qual provedor será integrado depois do mock e quais meios de pagamento serão aceitos (Pix, cartão, outros)?
2. Qual o percentual da comissão, base de cálculo, taxas adicionais e política/momento de repasse?
3. Ao integrar um provedor real, como executar a compensação financeira se a aprovação chegar após a reserva expirar? A regra atual não confirma a venda nem readquire estoque.
4. Quais são a janela e as condições para cancelamento/reembolso; quem decide e qual o efeito sobre estoque e QR?
5. Quais filtros e informações a descoberta de eventos precisa oferecer? Quais são os campos e limites obrigatórios do evento?
6. Como representar lotes, limites por comprador, preço/taxas, descontos e eventual mudança de lote?
7. Como produtores convidam operadores de entrada e como revogam acesso? Scanner precisa funcionar sem rede?
8. Quais notificações por e-mail são necessárias além da confirmação? Qual provedor será integrado?
9. Quais dados fiscais, termos de uso, privacidade e requisitos de proteção de dados são exigidos para operar no Brasil?
10. Quais permissões ADMIN de edição e metadados de acessibilidade de assento são necessários além das regras já implementadas?

## 6. Diretrizes de implementação e Spec Driven Development

- Transformar cada fluxo acima em especificação com ator, pré-condições, regras, estados, erros, permissões e critérios de aceite antes de implementar.
- Versionar contratos de API e atualizar Angular e API juntos quando payloads ou estados mudarem.
- Manter regras no backend; o frontend não é autoridade para papel, preço, estoque, pagamento, QR ou permissão de scanner.
- Persistir pedidos, pagamentos simulados, ingressos e validações; H2 em arquivo mantém dados localmente, mas não substitui um banco de produção multi-instância.
- Incluir testes de autorização, concorrência de estoque, transições de estado, idempotência, repetição de QR e contrato do mock substituível.
- Usar configurações e segredos por ambiente; `TOKEN_SECRET` é obrigatório e deve ser fornecido fora do repositório.
- Atualizar [Arquitetura do Sistema](Arquitetura%20do%20Sistema.md) e READMEs locais na mesma mudança quando novas responsabilidades entrarem no código.

Os itens 1 a 6 estão implementados no escopo confirmado. Provedor de pagamento/e-mail real, comissão, repasse, política para tickets já usados em cancelamento e pós-venda financeiro real permanecem trabalho futuro.