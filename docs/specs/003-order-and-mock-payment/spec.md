# Feature Specification: Order and Mock Payment

**Feature Branch**: `003-order-and-mock-payment`

**Created**: 2026-10-06

**Status**: Implemented (real-provider fees/refunds remain out of scope)

**Input**: User description: "Implementar o terceiro item da sequência: checkout autenticado de um evento, criação de pedidos, simulação configurável de pagamento aprovado/recusado/pendente, controle concorrente de estoque e idempotência."

**Source material**: [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md), [Arquitetura do Sistema](../../Arquitetura%20do%20Sistema.md), [Foundation and Security](../001-foundation-security/spec.md), and [Events and Offers](../002-events-and-offers/spec.md).

## User Scenarios & Testing

### User Story 1 - Buyer creates an order with reserved inventory (Priority: P1)

As an authenticated buyer, I can place one order for tickets from a single published event and see the selected categories/seats and total so that the requested inventory is held while payment is pending.

**Why this priority**: Event and offer data are now available, but the marketplace cannot complete a purchase journey without a durable order and inventory protection.

**Independent Test**: Publish an event with general-admission inventory and assigned seats, authenticate as a buyer, create an order using multiple categories from that event, and verify the order is pending, its total is calculated from the current BRL-cent prices, and the requested inventory is reserved for no more than 15 minutes.

**Acceptance Scenarios**:

1. **Given** an authenticated buyer and a published event with available offers, **When** the buyer submits an order containing one or more categories from that event, **Then** the system creates a pending order associated with that buyer and event and records each selected item and its price at order time.
2. **Given** an order contains tickets from more than one event, **When** the buyer submits it, **Then** the request is rejected and no inventory is reserved.
3. **Given** an order requests more than 10 tickets from one category, **When** it is submitted, **Then** the request is rejected without creating a partial order or reservation.
4. **Given** a general-admission category has sufficient stock, **When** an order is created, **Then** its requested quantity is reserved and unavailable to competing orders while the reservation is active.
5. **Given** an assigned-seat offer has specific available seats, **When** an order is created, **Then** each selected seat is reserved exclusively for that order.
6. **Given** an order remains unpaid for 15 minutes, **When** its reservation expires, **Then** the order becomes expired and its general-admission quantity/seats are released.
7. **Given** the same buyer retries order creation with the same `Idempotency-Key` and identical payload, **When** the request is repeated, **Then** the original order is returned without creating another order or reservation.
8. **Given** the same buyer reuses an `Idempotency-Key` with a different payload, **When** the request is submitted, **Then** it is rejected and no additional inventory is reserved.

### User Story 2 - Buyer receives a simulated payment result (Priority: P1)

As a buyer, I can complete the pending order against a payment mock that simulates approved, declined, or pending outcomes so the order lifecycle can be tested before a real provider is integrated.

**Why this priority**: The mock makes order states and inventory transitions demonstrable without making real charges, while an adapter boundary avoids binding order rules to the mock implementation.

**Independent Test**: For the same order flow, configure a test/development scenario to return each of the three payment outcomes. Verify the resulting order state and inventory effects, and verify the buyer cannot choose or override the simulated result in the request.

**Acceptance Scenarios**:

1. **Given** a pending order with an active reservation, **When** the configured mock scenario approves payment, **Then** the order becomes paid, the inventory remains committed to that order, and no QR ticket or email is issued by this feature.
2. **Given** a pending order with an active reservation, **When** the mock declines payment, **Then** the order is marked as payment declined and its reserved inventory is released.
3. **Given** a pending order with an active reservation, **When** the mock returns pending, **Then** the order remains pending and the existing 15-minute reservation deadline is not extended.
4. **Given** a pending order has expired and its inventory was released, **When** an approval arrives after the 15-minute deadline, **Then** the order is not marked paid and no inventory is committed or oversold; the late result is rejected/compensated.
5. **Given** the same payment result or notification is submitted more than once, **When** it is processed again, **Then** the operation is idempotent and does not duplicate the order, inventory commitment, or payment result.
6. **Given** the application is not running in a test/development mock scenario, **When** a buyer submits an order, **Then** the buyer cannot supply an outcome that forces approval, decline, or pending status.
7. **Given** an event is suspended or cancelled while it has pending orders, **When** the event state changes, **Then** pending orders are cancelled and their reservations are released immediately; paid orders are unchanged.
8. **Given** the event start time has arrived, **When** a buyer attempts to create a new order, **Then** the order is rejected.

### User Story 3 - Buyer views order history and current status (Priority: P2)

As an authenticated buyer, I can view only my own orders and their items, total, payment/order status, and reservation or completion time so I can tell whether a purchase completed or expired.

**Why this priority**: Orders are persisted records. Buyers need a reliable result after checkout, especially when a mock payment is pending or declined.

**Independent Test**: Create orders in pending, paid, declined, and expired states; authenticate as the order owner and verify all appear with correct totals/items/status. Authenticate as another buyer and verify the first buyer's orders are not disclosed.

**Acceptance Scenarios**:

1. **Given** a buyer has orders, **When** they request their order history, **Then** they see only orders belonging to their account with event, line items, price snapshots, total, and current status.
2. **Given** a buyer requests an order belonging to another account, **When** the API authorizes the request, **Then** access is denied or the order is not found and no order data is disclosed.
3. **Given** a buyer's order is pending, declined, expired, or paid, **When** it is shown in history, **Then** its displayed state matches the persisted state and never labels a pending/declined/expired order as paid.

### Edge Cases

- Two buyers request the last general-admission ticket at the same time; no more than one reservation may succeed.
- Two buyers request the same assigned seat at the same time; no more than one reservation may succeed.
- One order requests a valid quantity plus an unavailable category/seat; the request must be atomic and create no partial reservation.
- The event is suspended or cancelled after a pending order is created; pending reservations are cancelled/released, but paid orders remain unchanged.
- The event start time passes before a new checkout; sales are closed at `startsAt`.
- A category price changes after order creation; the order total and line-item price snapshot must not change.
- Payment declines immediately before the reservation deadline, or approval races with expiry/release.
- The mock fails, times out, or returns an invalid/unknown state; the order must not become paid and inventory must follow a deterministic documented state transition.
- A repeated create-order request is submitted after a network timeout; the client must reuse the required `Idempotency-Key` and identical payload to receive the original order.
- A buyer attempts to request 11 tickets in one category, a negative quantity, zero quantity, an invalid seat, or a seat from another event.
- A producer/admin or unauthenticated caller attempts to create an order or view another buyer's order history.
- A pending reservation expires while another transaction is attempting to reserve the newly released item.

## Requirements

### Functional Requirements

- **FR-001**: System MUST require an authenticated buyer to create an order.
- **FR-002**: Each order MUST contain tickets from exactly one event and MAY contain items from multiple categories of that event.
- **FR-003**: System MUST reject more than 10 tickets from any one category in a single order; quantity MUST be a positive integer.
- **FR-004**: System MUST validate that the event is eligible for sale, each selected category belongs to the event, and each selected seat is available and belongs to the selected category.
- **FR-005**: System MUST calculate order totals in BRL cents from the price effective when the order is created and persist a unit-price snapshot for every line item.
- **FR-006**: System MUST create the order and inventory reservations atomically; a failed order MUST NOT retain partial reservations.
- **FR-007**: System MUST guarantee that concurrent orders cannot reserve or confirm more general-admission tickets than configured quantity or reserve the same seat more than once.
- **FR-008**: System MUST hold pending order inventory until the earlier of 15 minutes after order creation or the event's `startsAt`; expiration MUST release the held quantity/seats.
- **FR-009**: System MUST support mock payment results of approved, declined, and pending in test/development scenarios only; buyers MUST NOT control the result through public order fields.
- **FR-010**: An approved payment received while the reservation is valid MUST mark the order paid and commit its reserved inventory.
- **FR-011**: A declined payment MUST mark the order as declined and release its reserved inventory.
- **FR-012**: A pending payment MUST leave the order pending without extending the original reservation deadline.
- **FR-013**: An approval received after order expiry MUST NOT mark the order paid or reacquire inventory; the late payment result MUST be rejected/compensated without overselling.
- **FR-014**: Payment result processing MUST be idempotent; replaying the same result MUST NOT duplicate order transitions or inventory changes, and a conflicting result after completion MUST be rejected.
- **FR-015**: System MUST expose the authenticated buyer's order history and order detail, including event, items, price snapshots, total, timestamps, and order/payment status.
- **FR-016**: System MUST authorize order/history reads against the order owner; users MUST NOT read another account's orders.
- **FR-017**: System MUST provide a replaceable payment boundary so the mock can later be replaced by a real provider without changing order invariants.
- **FR-018**: This feature MUST NOT issue ticket QR codes or send purchase emails; ticket issuance/delivery belongs to the next feature.
- **FR-019**: Provider selection, real payment methods, fees, taxes, and commission/payout calculation are outside this mock-payment feature. **[NEEDS CLARIFICATION: whether any buyer-facing service fee must be displayed before future real-provider checkout].**
- **FR-020**: System MUST persist orders, line items, payment attempts/results, and reservation expiry data through the existing Flyway-managed persistence foundation.
- **FR-021**: System MUST select mock outcomes through the replaceable `PaymentGateway` test fixture or dev/test configuration only; no buyer request field or public endpoint may choose the result.
- **FR-022**: Suspending/cancelling an event MUST immediately cancel its pending orders and release their reservations; paid orders remain unchanged. New orders MUST be rejected at/after the event's `startsAt`.
- **FR-023**: System MUST require an `Idempotency-Key` header for order creation, scoped to the authenticated buyer. Reuse with identical payload MUST return the original order; reuse with a different payload MUST be rejected.

### Key Entities

- **Order**: A buyer-owned purchase intent for exactly one event, with created/updated/expiry times, BRL-cent total, and lifecycle state.
- **Order Item**: A line item for one ticket category and optionally specific assigned seats; records quantity, seat identities, and unit-price snapshot.
- **Inventory Reservation**: A temporary hold against general-admission quantity or specific seats, tied to one pending order and expiring no later than 15 minutes after creation.
- **Payment Attempt**: A replaceable payment operation associated with one order, including simulator/provider identity, result state, and timestamps; real provider details are outside this feature.
- **Payment Result**: An approved, declined, or pending outcome applied idempotently to the order and its reservation.
- **Buyer**: An authenticated `USER` account that owns and may read its order/history; producer/admin permissions do not grant access to another buyer's orders.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Every accepted order references exactly one event, and every line item records the unit price and category as they were when the order was created.
- **SC-002**: In concurrent tests for one remaining general-admission ticket or one seat, at most one order obtains an active reservation.
- **SC-003**: Every pending reservation is released no later than the earlier of 15 minutes after order creation or the event start, and is released immediately if the event is suspended/cancelled.
- **SC-004**: Across approved, declined, pending, expired, repeated, and late-approval payment scenarios, no order is incorrectly marked paid and no inventory is oversold.
- **SC-005**: Reprocessing a payment result produces no duplicate state transition or inventory mutation.
- **SC-006**: Buyers can retrieve their own order history and cannot retrieve another buyer's order data.
- **SC-007**: No QR ticket or purchase email is issued by this feature before the separate ticket-delivery feature.

## Assumptions

- This feature depends on producer-created events, categories, configured stock, and seats from [002-events-and-offers](../002-events-and-offers/spec.md).
- Checkout requires the existing authenticated buyer account; guest checkout is not included.
- One order can include multiple categories/seats but only from one event.
- The per-category per-order limit is 10 tickets. No per-buyer lifetime/daily limit was requested.
- Inventory is reserved until the earlier of 15 minutes after order creation or event start; suspension/cancellation releases pending inventory immediately.
- An approved result after expiry does not restore or overbook inventory; pending event reservations cancelled by moderation remain cancelled.
- The mock runs only under dev/test profiles, uses a replaceable gateway/fixture with environment default outcome, and cannot let a buyer force a result.
- Order creation requires `Idempotency-Key`; the same key is reusable only with the same payload.
- An approved mock result changes the order to paid, but QR issuance, confirmation email, refunds, and real-provider calls belong to later features.
- Prices come from current event offers in BRL cents. Service fees/taxes/commission are not added by this feature; any buyer-facing fee policy remains open.
- **[NEEDS CLARIFICATION: define client-facing service-fee/tax display for future real-provider checkout and provider-specific handling of refunds/compensation].**