# Feature Specification: Post-Sale and Finance

**Feature Branch**: `006-post-sale-and-finance`

**Created**: 2026-10-07

**Status**: Implemented (mock refunds and gross reports; financial policy decisions remain open)

**Input**: User request to implement item 6 in the recommended sequence in [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md). Confirmed decisions: automatic per-ticket refunds for paid unused tickets before event start; event cancellation automatically creates mock refunds for all paid unused tickets; provider/money movement remains mock-only; gross/refund reports exclude undefined commission/payout.

**Source material**: [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md), [Arquitetura do Sistema](../../Arquitetura%20do%20Sistema.md), [Order and Mock Payment](../003-order-and-mock-payment/spec.md), [Ticket Delivery](../004-ticket-delivery/spec.md), and [Event Entry Validation](../005-entry-validation/spec.md).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Buyer requests an eligible ticket refund (Priority: P1)

As a buyer, I can request a refund for a specific paid ticket that has not been used before the event starts so that I can cancel an eligible admission without cancelling unrelated tickets in the same order.

**Why this priority**: The system sells multiple individual tickets per order. A post-sale action must identify the specific admission and must not silently refund tickets that have already been used.

**Independent Test**: Create and pay for an order with multiple tickets, request a refund for one unused ticket before the event start, and verify that only that ticket is marked refunded, the amount uses its order-time BRL-cent price, and the sibling tickets remain active.

**Acceptance Scenarios**:

1. **Given** a buyer owns a `PAID` order with an unused ticket and the event has not started, **When** they request cancellation/refund for that ticket, **Then** the request is automatically approved in the mock flow and a simulated refund record is created.
2. **Given** an order contains several unused tickets, **When** the buyer requests a refund for one selected ticket, **Then** only the selected ticket is refunded and other tickets remain eligible for entry/refund.
3. **Given** a ticket has already been checked in, **When** its buyer requests a refund, **Then** the request is rejected and no refund record or amount is created.
4. **Given** an order is pending, declined, expired, or cancelled without a paid ticket, **When** a refund is requested, **Then** it is rejected without changing the order or ticket state.
5. **Given** the event's local start time has arrived, **When** a buyer requests a refund, **Then** the request is rejected.
6. **Given** the same refund request is repeated or raced with another refund/check-in request, **When** the system processes it, **Then** no ticket receives duplicate refunds and the ticket cannot be both refunded and successfully checked in.

### User Story 2 - Buyer is protected when an event is cancelled (Priority: P1)

As a buyer, I receive a simulated refund record for every paid, unused ticket when the producer or an administrator cancels the event so that cancelled admissions are not presented as usable.

**Why this priority**: Event cancellation affects every admission and should not require each buyer to submit a separate request for tickets that remain unused.

**Independent Test**: Create a paid order with multiple unused tickets, cancel the event as an authorized producer or administrator, and verify one simulated refund per eligible ticket, correct aggregate amounts, and rejection of those tickets by the entry-validation flow.

**Acceptance Scenarios**:

1. **Given** a producer cancels an event with paid, unused tickets, **When** cancellation commits, **Then** a mock refund is created automatically for each eligible ticket.
2. **Given** an administrator cancels an event with paid, unused tickets, **When** cancellation commits, **Then** the same automatic mock-refund rule applies and the actor is recorded.
3. **Given** an event has both unused and already-used tickets, **When** it is cancelled, **Then** unused tickets receive mock refunds; the treatment of already-used tickets follows an explicit policy decision and is not inferred here.
4. **Given** event cancellation or refund processing is retried, **When** the operation is repeated, **Then** no ticket receives a duplicate refund.
5. **Given** a ticket has a refund record, **When** an operator validates its QR, **Then** entry is rejected and the ticket is not reported as unused/valid.

### User Story 3 - Producer and administrator review post-sale totals (Priority: P2)

As a producer, I can review post-sale totals for my own events, and as an administrator I can review totals across events, so that recorded sales and simulated refunds can be reconciled.

**Why this priority**: The backlog calls for auditable gross sales, refunds, and producer amounts. Basic sales/refund reconciliation can be specified without inventing a commission rate or claiming that mock amounts represent money transferred.

**Independent Test**: Create paid orders, ticket-level mock refunds, and an event cancellation; compare event/admin report totals with the sum of persisted paid ticket snapshots and refund records. Verify a producer cannot view another producer's report.

**Acceptance Scenarios**:

1. **Given** a producer requests a report for an event they own, **When** it is generated, **Then** it includes gross paid ticket value, simulated refund value, remaining non-refunded gross value, and ticket/order counts.
2. **Given** an administrator requests a report, **When** it is generated, **Then** it can aggregate the same measures across events.
3. **Given** a producer requests a report for an event they do not own, **When** authorization is evaluated, **Then** no event or financial data is disclosed.
4. **Given** commission, service fees, and payout rules have not been configured, **When** a report is shown, **Then** it does not invent producer net payable, commission, or settlement values and clearly marks them unavailable/undefined.

### Edge Cases

- Buyer submits an empty, malformed, or foreign ticket identifier or requests a ticket belonging to another buyer.
- A ticket is used, refunded, already requested, or re-opened once by an ADMIN correction before a refund request.
- A refund request arrives at the exact event start boundary in `America/Sao_Paulo`.
- Check-in and refund race for the same ticket; only one terminal action may take effect.
- Producer/admin cancels an event while refund records are being created or retried.
- An event cancellation is repeated, or paid orders/tickets are paginated while batch refunds are processed.
- A ticket price differs from current event offer price; refund amount must use the ticket/order snapshot, not the current price.
- A database transaction fails partway through a multi-ticket event cancellation; reports and ticket states must not claim a partial batch completed silently.
- A ticket has already been used when the event is cancelled; the refund outcome requires the unresolved used-admission policy below.
- Refund adapter reports timeout, duplicate acknowledgement, or failure; mock states must not be described as real money movement.
- A service fee, tax, commission, payout, partial refund, or provider exchange rate would change a displayed amount.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an authenticated buyer to request a refund only for a ticket belonging to one of their own orders.
- **FR-002**: System MUST automatically approve a buyer-requested refund only when the ticket's order is `PAID`, the ticket is unused and not previously refunded, and the request is received before the event's local start time.
- **FR-003**: Buyer refund requests MUST target individual tickets so one eligible ticket can be refunded without refunding other tickets in the same order.
- **FR-004**: A refunded ticket MUST no longer be eligible for entry validation; ticket use and refund state MUST be mutually exclusive under concurrent requests.
- **FR-005**: The simulated refund amount MUST use the unit-price snapshot recorded for the ticket/order in BRL cents, not the current offer price.
- **FR-006**: Cancelling an event by its producer or ADMIN MUST automatically create one mock refund for each paid, unused ticket associated with that event.
- **FR-007**: Refund processing MUST be idempotent; repeated requests, event-cancellation retries, or provider/mock notifications MUST NOT create duplicate refunds.
- **FR-008**: System MUST persist refund actor/source, ticket/order/event, amount, status, timestamps, and the reason/source event (buyer request or event cancellation) for audit.
- **FR-009**: Producer reports MUST be limited to events they own; ADMIN reports MAY aggregate across events. Buyers MUST NOT access producer/admin financial reports.
- **FR-010**: Reports MUST reconcile gross paid ticket value, simulated refund value, and remaining non-refunded gross value from persisted ticket price snapshots and refund records.
- **FR-011**: System MUST NOT calculate or display a commission, service fee, tax, producer net payable, or payout schedule as a confirmed value until its business rule is configured.
- **FR-012**: This feature's refund execution MUST use a mockable boundary and MUST NOT claim to move money or issue a real refund.
- **FR-013**: Selection/configuration of a real payment/refund provider and actual refund or payout execution are outside this specification until provider, methods, and business policies are confirmed.
- **FR-016**: Refund execution MUST cross a replaceable `RefundGateway` boundary; the configured mock MUST label outcomes `SIMULATED` and MUST NOT represent that money moved.
- **FR-014**: **[NEEDS CLARIFICATION: Define whether and how a ticket already used before event cancellation is refunded; this changes the ticket/refund lifecycle and report totals.]**
- **FR-015**: **[NEEDS CLARIFICATION: Define commission rate and calculation base, treatment of service/provider fees and taxes, and payout timing/recipient; without these, producer net payable and repasse cannot be computed.]**

### Key Entities *(include if feature involves data)*

- **Refund Request**: A buyer's request for one or more specific eligible tickets, with buyer, reason if required, requested time, decision/status, and resulting ticket-level refund records.
- **Ticket Refund**: An idempotent simulated refund for one issued ticket, linked to its order/event and price snapshot, with source, amount, status, actor, and timestamps.
- **Event Cancellation Refund Batch**: The set of ticket refunds automatically created for paid, unused tickets when an authorized producer/admin cancels an event.
- **Financial Report**: A producer-scoped or admin-wide aggregation of gross paid value, simulated refunds, remaining gross value, and ticket/order counts. Commission/net payable remain unavailable until defined.
- **Issued Ticket**: The individual ticket from [004-ticket-delivery](../004-ticket-delivery/spec.md), with use state from [005-entry-validation](../005-entry-validation/spec.md); a ticket cannot be both successfully checked in and refunded.
- **Order/Order Item**: The paid buyer order and BRL-cent unit-price snapshots from [003-order-and-mock-payment](../003-order-and-mock-payment/spec.md), used as the source for mock refund amounts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero refund records are created for unpaid, foreign-owned, used, already-refunded, or post-event-start tickets.
- **SC-002**: In concurrent refund/check-in tests for the same ticket, at most one terminal outcome succeeds; the ticket is never both refunded and consumed.
- **SC-003**: Cancelling an event creates exactly one mock refund per paid, unused ticket and no duplicate refund after retries.
- **SC-004**: Ticket-level refund amounts equal their persisted BRL-cent unit-price snapshots; event/admin aggregates reconcile exactly to those records.
- **SC-005**: A producer can access reports only for owned events; buyers receive no financial-report data.
- **SC-006**: No UI/API labels a mock refund as a real financial transfer or displays an undefined commission/payout amount as confirmed.

## Assumptions

- This feature depends on buyer-owned orders and ticket price snapshots from [003-order-and-mock-payment](../003-order-and-mock-payment/spec.md), issued tickets from [004-ticket-delivery](../004-ticket-delivery/spec.md), and ticket-use state from [005-entry-validation](../005-entry-validation/spec.md).
- The user's confirmed buyer-request rule is automatic approval per ticket when its order is paid, the ticket has not been used, and the request arrives before the event's local start. Event timezone follows the existing `America/Sao_Paulo` rule.
- The user's confirmed event-cancellation rule is automatic creation of mock refunds for every paid, unused ticket. Refunds for already-used tickets remain unresolved.
- The user selected mockable refunds and deferred a real provider; no real charge/refund/payout occurs in this scope.
- Reports may show gross paid value, simulated refunds, remaining gross value, and counts; commission/net payable are not calculable until business rules are decided.
- **[NEEDS CLARIFICATION: What commission rate and calculation base apply, and are service/provider fees or taxes refundable?]**
- **[NEEDS CLARIFICATION: What happens to a ticket already used before producer/admin event cancellation?]**
- **[NEEDS CLARIFICATION: Which provider/methods will be selected later, and what actual-refund/payout status callbacks and retry/reconciliation behavior will be required?]**
- Report date filters, export format, and settlement cadence remain outside confirmed input and should not be invented in this draft.

**Implementation status**: FR-001 through FR-013 and FR-016 are implemented for the mock scope. FR-014/FR-015 and their clarifications remain open business decisions; real refunds, commission, and payout are not implemented.