# Modelo de domínio persistido

Contém usuários/papéis, eventos/ofertas/assentos, pedidos, tickets, refunds e operação de entrada: inclui `IssuedTicket`, `TicketRefund`, `EventStaffInvitation`, `OfflineEntryManifest`, `EntryCheckIn` e `EntryCheckInCorrection`.

## Dependências e relações

`Usuario` é persistido por `UsuarioRepository`; `Event` pertence a um produtor. `PurchaseOrder` contém itens com snapshots de preço/categoria; cada `IssuedTicket` representa uma unidade e guarda QR opaco/hash único, uso atual e snapshot opcional do assento. Check-ins e correções mantêm auditoria sem apagar o registro original.

## Arquivos críticos e observações

`V3__events_and_offers.sql` até `V11__ticket_refunds.sql` criam o schema de eventos, pedidos/tickets, uso, grants event-scoped, manifestos hash-only, auditoria e refund único por ticket.