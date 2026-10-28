# Feature Specification: Ticket Delivery

**Feature Branch**: `004-ticket-delivery`

**Created**: 2026-10-07

**Status**: Implemented

**Input**: User request to implement item 4 in the recommended sequence in [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md). Confirmed decisions: one ticket per purchased unit; opaque QR identifier; QR included in the email body; mock adapter in development/test; no automatic retry; current event details on tickets.

**Source material**: [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md), [Arquitetura do Sistema](../../Arquitetura%20do%20Sistema.md), [Order and Mock Payment](../003-order-and-mock-payment/spec.md), [Events and Offers](../002-events-and-offers/spec.md).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Buyer receives a ticket for each paid admission (Priority: P1)

As a buyer with a paid order, I receive a distinct digital ticket for every purchased admission so each person or seat can be presented independently at a future event-entry check.

**Why this priority**: A paid order currently has no admission credential. Issuing tickets is the core value of this feature and a prerequisite for the later scanner/entry-validation feature.

**Independent Test**: Create paid orders for general-admission and assigned-seat categories. Verify that each purchased unit has exactly one ticket, each ticket has its own QR identifier, and assigned-seat tickets point to their purchased seat.

**Acceptance Scenarios**:

1. **Given** an order becomes paid, **When** ticket issuance completes, **Then** exactly one ticket is available for each unit in the order, including general-admission units.
2. **Given** an order contains assigned seats, **When** its tickets are issued, **Then** each ticket identifies exactly the seat represented by its order item snapshot.
3. **Given** an order is pending, declined, expired, or cancelled, **When** its ticket list is requested or issuance is considered, **Then** no ticket or usable QR is issued for that order.
4. **Given** the same approval is processed more than once, **When** issuance is repeated, **Then** the original tickets remain associated with the order and no duplicate tickets are created.

### User Story 2 - Buyer views their tickets from order history (Priority: P1)

As an authenticated buyer, I can open a paid order and view its tickets and QR codes so I can retrieve the admissions after checkout without depending on the confirmation email.

**Why this priority**: Email delivery can fail or be unavailable in the mock environment; the authenticated account must remain the durable place to retrieve issued tickets.

**Independent Test**: Issue tickets for a paid order, authenticate as its owner, and verify ticket details and QR data are displayed with the order. Authenticate as another buyer and verify that no order or ticket data is disclosed.

**Acceptance Scenarios**:

1. **Given** a buyer owns a paid order with issued tickets, **When** they view that order in their history, **Then** the response/page presents every ticket with its event, category, and assigned seat when applicable.
2. **Given** a buyer requests tickets belonging to another account, **When** access is checked, **Then** access is denied or the resource is not found without disclosing ticket data.
3. **Given** an order is not paid, **When** the buyer opens it, **Then** the interface does not display an issued ticket or QR.

### User Story 3 - Buyer confirmation is passed to the email adapter (Priority: P2)

As a buyer, I receive a purchase confirmation through an email delivery boundary so ticket delivery can be tested now and connected to a real provider later.

**Why this priority**: Email is a useful secondary delivery channel, but tickets must remain accessible in the account independently of email availability.

**Independent Test**: In a development/test scenario, pay an order and verify that the mock adapter captures a confirmation request associated with the buyer and issued tickets, without sending an external email.

**Acceptance Scenarios**:

1. **Given** tickets have been issued for a paid order, **When** the confirmation flow runs in development/test, **Then** the mock email adapter receives a message associated with the order and its tickets.
2. **Given** the mock email adapter fails, **When** delivery is attempted, **Then** the issued tickets remain available to the buyer in their authenticated account.
3. **Given** the application has no configured real email provider, **When** a ticket is issued, **Then** no external email is sent and the mock adapter is used only in its permitted development/test environment.

### Edge Cases

- Payment approval is replayed or races with another processing attempt; ticket issuance must not create duplicate tickets.
- An order contains multiple categories, general-admission quantities, and assigned seats; every purchased unit must map to exactly one ticket.
- A paid order has no items or inconsistent seat snapshots due to corrupt persisted data; issuance must fail safely and must not expose a partial ticket set.
- An unauthenticated user or a different buyer guesses an order/ticket identifier; no ticket or QR data is disclosed.
- A ticket QR is copied or forwarded; its opaque identifier must not reveal personal data, price, or trusted payment state.
- Email delivery fails or times out after ticket issuance; ticket availability in the account is unaffected.
- Event title, date/time, or venue changes after purchase; the ticket reflects current event details while category and assigned seat remain from the order snapshot.
- A future cancellation/refund changes the order after issuance; revocation behavior is not defined by this feature and must be handled by the post-sale policy before implementation of that flow.
- A buyer changes their account email, or buys tickets for other attendees; recipient and transfer behavior need product decisions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST issue tickets only for an order whose persisted status is `PAID`.
- **FR-002**: System MUST issue exactly one ticket for each purchased unit in a paid order, including each general-admission quantity.
- **FR-003**: For assigned-seat items, each issued ticket MUST be associated with exactly one seat from the order's seat snapshot; the same seat MUST NOT be represented by multiple tickets for that order.
- **FR-004**: Each ticket MUST have a unique opaque QR identifier that is not a sequential order/ticket identifier and does not encode personal data, price, or authoritative payment/validity state. The identifier is intended for server-side validation by the later entry-validation feature.
- **FR-005**: System MUST NOT issue or display tickets/QR identifiers for pending, declined, expired, or cancelled orders.
- **FR-006**: System MUST expose issued tickets through the buyer's authenticated order history/detail and MUST authorize every ticket read against the owning buyer.
- **FR-007**: A buyer MUST NOT be able to retrieve another buyer's tickets by changing an order or ticket identifier.
- **FR-008**: Ticket issuance MUST be idempotent with respect to repeated processing of the same paid-order approval; repeated processing MUST NOT create additional tickets.
- **FR-009**: After ticket issuance, System MUST request a purchase confirmation through a replaceable email-delivery boundary.
- **FR-010**: Development/test email behavior MUST use a mock adapter that captures or records the requested message without sending email to an external recipient.
- **FR-011**: Failure of the email adapter MUST NOT invalidate or hide tickets already issued to the buyer.
- **FR-012**: Real email-provider configuration and delivery are outside this feature until a provider and operating requirements are confirmed.
- **FR-013**: QR scanning, admission validation, offline operation, ticket transfer, refunds, and post-sale cancellation/revocation are outside this feature and depend on later product decisions/features.
- **FR-014**: The confirmation request MUST be addressed to the buyer's registered account email and include each ticket's QR value in its message content.
- **FR-015**: The confirmation request MUST be dispatched after the order transaction commits. A failed mock delivery MUST be logged, MUST NOT trigger an automatic retry, and MUST NOT affect ticket access.
- **FR-016**: Ticket event title, start time, and venue MUST reflect current event data; category and assigned-seat details MUST reflect the order snapshots.

### Key Entities *(include if feature involves data)*

- **Ticket**: A digital admission for one purchased unit, associated with one paid order and, when assigned seating applies, one seat snapshot; includes a unique opaque QR identifier and issuance time.
- **QR Identifier**: A non-sequential opaque reference associated with one ticket and intended for later server-side validation; it is not itself proof of current validity without server verification.
- **Email Delivery Request**: A request to deliver a purchase confirmation to the buyer, associated with an order and its issued tickets; provider, message format, and retry lifecycle are not yet specified.
- **Buyer**: The account that owns the order and is authorized to view its tickets.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every paid order has exactly as many tickets as purchased units; every non-paid order has zero issued tickets.
- **SC-002**: Every assigned-seat ticket maps to one distinct seat from its order, and no ticket is missing or duplicated when an order contains multiple categories.
- **SC-003**: Replaying payment approval produces no additional ticket records or QR identifiers.
- **SC-004**: In authorization tests, the order owner can retrieve all of their tickets and a different account cannot retrieve any of them.
- **SC-005**: Development/test confirmation requests reach the mock email adapter after commit without external email delivery; a simulated failure is logged, is not retried automatically, and does not remove buyer access to issued tickets.
- **SC-006**: QR identifiers do not reveal personal data or sequential order/ticket identifiers and are unique across issued tickets.

## Assumptions

- This feature depends on paid orders, item/seat snapshots, and buyer ownership from [003-order-and-mock-payment](../003-order-and-mock-payment/spec.md), and event/offer data from [002-events-and-offers](../002-events-and-offers/spec.md).
- The user's confirmed rule is one ticket per purchased unit, including general admission; assigned-seat tickets map to the corresponding seat.
- The user's confirmed QR choice is an opaque identifier validated by the server; actual scanning/validation belongs to the later entry-operation feature.
- The user's confirmed email boundary uses a mock adapter in development/test; selecting and operating a real provider is deferred.
- Buyers retrieve tickets from their authenticated order history even if email delivery fails.
- The confirmation is addressed to the buyer's registered email and contains each QR value; no attendee assignment or ticket transfer is included.
- Delivery is attempted once after database commit. Failures are logged, are not automatically retried, and do not revoke issued tickets.
- Event title, start time, and venue are read from the current event; category and seat labels remain from order snapshots.
- Refund/cancellation invalidation and ticket transfer are not inferred; they require decisions in the post-sale and entry-validation scope.