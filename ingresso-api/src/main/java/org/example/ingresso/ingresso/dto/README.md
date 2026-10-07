# DTOs da API

Define os contratos de entrada e saída usados pelos controllers; records mantêm os dados de transporte sem expor deliberadamente a entidade JPA.

## Contratos existentes

- `CredentialRequest`: e-mail e senha, com validações Jakarta.
- `CreateUsuarioRequest`: nome, e-mail e senha, além do campo legado opcional `perfil`; o service ignora esse campo e cria somente contas `USER`.
- `AuthResponse`: token JWT.
- `UsuarioResponse`: id, nome, e-mail e conjunto de papéis, usado pela listagem ADMIN.
- `CreateEventRequest`: campos do evento, categorias com preço em centavos BRL e setores/fileiras/assentos; usado na criação/edição por produtor.
- `EventResponse` e `EventSummaryResponse`: detalhe com categorias/mapa e resumo paginado para catálogo e backoffice.
- `CreateOrderRequest`: evento e itens/categorias, quantidade e IDs dos assentos escolhidos.
- `OrderResponse`: status, total em centavos, snapshots de preço/categoria/assentos e tentativa de pagamento.
- Categorias e assentos usam `AdmissionMode`, `EventStatus` e `SeatStatus` do modelo.

## Dependências e cuidados

Depende de Jakarta Validation e, para `UsuarioResponse`, do enum `model.enums.UsuarioPerfil`. Controllers consomem requisições e serviços montam as respostas. Alterar campos é mudança de contrato para clientes, incluindo `../ingresso-web`; atualizar testes e documentação na mesma alteração.