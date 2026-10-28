# Feature Specification: Events and Offers

**Feature Branch**: `002-events-and-offers`

**Created**: 2026-10-06

**Status**: Implemented (product-policy clarifications remain listed below)

**Input**: User description: "Implementar a segunda funcionalidade da sequência: produtor cadastra/publica evento, configura tipos de ingresso, preço, quantidade e opção de assento; comprador encontra evento e consulta disponibilidade."

**Source material**: [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md), [Arquitetura do Sistema](../../Arquitetura%20do%20Sistema.md), and [Foundation and Security](../001-foundation-security/spec.md).

## User Scenarios & Testing

### User Story 1 - Producer publishes an event with ticket offers (Priority: P1)

As an authorized producer, I can create and publish an event with its public details and ticket categories so buyers can discover what is being offered.

**Why this priority**: Events and offers are the marketplace's primary inventory. Publishing directly without an approval queue was confirmed for the initial product scope.

**Independent Test**: Authenticate as a producer, create a complete event with at least one non-assigned-seat category, publish it, and verify the public event record contains the submitted details and offer. Verify a buyer and an unrelated producer cannot create or modify it.

**Acceptance Scenarios**:

1. **Given** an authenticated producer, **When** they submit a valid event with title, description, HTTPS image URL, date/time, physical venue details, age classification, organizer, and at least one ticket category, **Then** the event is published without requiring administrator approval and is associated with that producer.
2. **Given** a buyer or unauthenticated client, **When** they attempt to create or edit an event through the API, **Then** the request is denied and no event data changes.
3. **Given** a producer attempts to modify an event owned by a different producer, **When** the API authorizes the operation, **Then** access is denied and the event remains unchanged.
4. **Given** an event submission is missing a confirmed required field or contains an invalid offer quantity, **When** it is submitted, **Then** publication fails with a validation response and the event is not visible publicly.

### User Story 2 - Buyer discovers an event and checks its offers (Priority: P1)

As a prospective buyer, I can view a public list of published events, open an event detail page, and see its ticket categories, prices, and availability before the checkout feature is implemented.

**Why this priority**: The confirmed product direction is a marketplace. Producers need a buyer-visible result from publishing, and the next purchase feature depends on event and offer discovery.

**Independent Test**: Publish an event as a producer, then access the public list and detail without signing in. Verify the event's public details and configured offers are shown, while unpublished or unauthorized data is not exposed.

**Acceptance Scenarios**:

1. **Given** one or more published events, **When** a visitor opens the public event list, **Then** each eligible event appears once and can be opened without authentication.
2. **Given** a published event, **When** a visitor opens its detail, **Then** the event details, ticket categories, prices, and availability information are presented.
3. **Given** an event has not been published or is not eligible for public visibility, **When** a visitor requests it from the public catalog, **Then** its details are not disclosed.
4. **Given** an event offer has configured quantity, **When** a visitor views its details before order management exists, **Then** the configured quantity and available/sold-out state are shown; a later order feature will decrement availability.

### User Story 3 - Producer configures assigned seats (Priority: P2)

As a producer organizing an event with assigned seating, I can configure a seat map and associate saleable seats with the event so the event can support assigned-seat offers.

**Why this priority**: Assigned seating is a confirmed sales mode. The producer-configured visual map is more specialized than general admission but remains part of this feature.

**Independent Test**: Create an event with a visual map of sectors, rows, and generated seats; assign each seat to a ticket category; verify unique identities, category prices, and public availability. Reject duplicate seat identities and seats assigned to another event.

**Acceptance Scenarios**:

1. **Given** an authorized producer, **When** they configure sectors, rows, and seat counts in the visual editor, **Then** the map is associated with the event and generates individually identifiable seats.
2. **Given** a map contains duplicate seat identities or invalid seat references, **When** the producer attempts to publish it, **Then** publication is rejected and no invalid seat is exposed as available.
3. **Given** a buyer views an assigned-seat event, **When** they inspect the event details, **Then** the configured map, each seat's category/price, and configured availability are visible without granting them permission to change the map.
4. **Given** a seat has already been sold by a later order feature, **When** the producer edits the event or map, **Then** that sold seat and its purchase record cannot be reassigned or removed. **Dependency: sale state is introduced by the order feature.**

### User Story 4 - Producer updates event details without rewriting sold offers (Priority: P2)

As the event owner, I can correct editorial details after publication while preserving the price and quantity commitments attached to tickets already sold.

**Why this priority**: Event information can change, but buyers' existing purchases must remain accurate. The interview confirmed editorial changes may remain free while sold offer terms are protected.

**Independent Test**: Publish an event, record a ticket sale through the order feature or a controlled fixture, update an editorial field, and verify the public detail changes while the sold ticket's recorded price and offer commitment remain unchanged.

**Acceptance Scenarios**:

1. **Given** a published event owned by the producer, **When** they update an editorial field such as description or image, **Then** the updated information is reflected publicly.
2. **Given** an offer has sold tickets, **When** the producer changes its current price, **Then** existing purchase records retain the price agreed at purchase and the change applies only to future sales, subject to the unresolved offer policy.
3. **Given** an offer has sold or committed inventory, **When** the producer reduces its total quantity below that amount, **Then** the update is rejected.
4. **Given** an administrator suspends or cancels a published event, **When** the public list is requested, **Then** the event is absent from the active list; a cancelled event detail remains accessible with a cancellation notice and no available offers.

### Edge Cases

- A buyer requests an unpublished event, a nonexistent event, or an event whose date has passed.
- A producer submits missing, malformed, or excessively long event content; duplicate categories; negative, zero, or excessively large prices/quantities.
- Two producers attempt to create events with the same title, or one producer uses the same category label more than once. Uniqueness rules are not yet specified.
- A map has duplicate seats, no saleable seats, seats not linked to a category, or more saleable seats than the offer capacity.
- An event's local time crosses an invalid or ambiguous time-zone transition.
- The producer edits a price or quantity after sales; historic purchase values and sold inventory must not be rewritten.
- A producer attempts to update another producer's event or a buyer attempts to modify any event data.
- A suspended event's public detail visibility and post-suspension actions. **[NEEDS CLARIFICATION: suspended-event visibility not specified].**
- An HTTPS image URL is invalid, unavailable, or inaccessible to public buyers.
- Availability is queried while orders are being created by the next feature; this feature alone has no order records to decrement configured inventory.
- A producer changes a category price after sales; existing order records and tickets must retain their recorded price.

## Requirements

### Functional Requirements

- **FR-001**: System MUST allow an authenticated account with `PRODUCER` permission to create an event.
- **FR-002**: System MUST associate each event with its creating producer and MUST authorize edits against that ownership on the server.
- **FR-003**: System MUST allow a producer to publish a valid event directly without administrator review.
- **FR-004**: A published event MUST expose the confirmed public fields: title, description, image, date/time, location, age classification, and organizer.
- **FR-005**: System MUST require an event to have at least one configured ticket category before it is publicly published.
- **FR-006**: System MUST support multiple ticket categories per event, each with one configured price and quantity; automatic lot transitions are outside this feature.
- **FR-007**: System MUST represent prices in Brazilian Real (BRL) cents, allow zero-price offers, require positive configured quantities, and reject negative or malformed prices/quantities. Maximum quantity remains **[NEEDS CLARIFICATION: per-category maximum not specified].**
- **FR-008**: System MUST expose a public list of eligible published events and a public detail view without requiring buyer authentication; discovery filters are outside the confirmed MVP scope.
- **FR-009**: System MUST expose offer price, configured quantity, and available/sold-out state on the event detail view without allowing public clients to modify offers. Until order management is added, all configured inventory is available.
- **FR-010**: System MUST support both general-admission offers and producer-configured assigned-seat maps.
- **FR-011**: Every saleable seat MUST have a unique identity within its event and MUST be associated with no more than one active offer.
- **FR-012**: System MUST prevent a producer from creating, publishing, or editing events owned by another producer. ADMIN MUST be able to suspend or cancel events; whether ADMIN can create/edit producer-owned event content remains **[NEEDS CLARIFICATION: admin edit powers not specified].**
- **FR-013**: System MUST allow the event owner to edit editorial details after publication.
- **FR-014**: System MUST preserve the agreed price and event/offer reference for any ticket already sold; changing the current offer price MUST NOT rewrite prior sales.
- **FR-015**: System MUST reject an offer quantity reduction below the quantity already sold or otherwise committed by the order feature.
- **FR-016**: System MUST persist event, category, offer, and seat-map data across API restarts using the existing Flyway-managed persistence foundation.
- **FR-017**: System MUST accept an HTTPS URL for the event image; image upload/storage is outside this feature.
- **FR-018**: System MUST record a physical venue name, street address, city, and state (UF), and interpret event date/time in `America/Sao_Paulo`. Remote events are outside this feature.
- **FR-019**: System MUST provide a visual editor that creates sectors, rows, and generated seat labels; each seat is assigned to one ticket category, from which it inherits its price.
- **FR-020**: Public event listing MUST include published events and exclude suspended/cancelled events. A cancelled event detail remains accessible with a cancellation notice and all offers unavailable. Past-event visibility and suspended-event detail visibility remain **[NEEDS CLARIFICATION: lifecycle visibility rules not fully specified].**
- **FR-021**: System MUST allow the producer owner to reactivate a suspended event; cancellation is terminal.

### Key Entities

- **Event**: A producer-owned public activity with title, description, image reference, date/time, location, age classification, organizer, publication state, and associated ticket categories.
- **Ticket Category**: A buyer-facing classification within an event, with one current price, configured quantity, and admission mode (general admission or assigned seat).
- **Offer**: The sellable terms for a ticket category, including price and quantity. Historical orders must retain their agreed terms when a current offer changes.
- **Seat Map**: A producer-configured visual layout of sectors, rows, and generated seats; each seat references one ticket category.
- **Seat**: A uniquely identifiable place within one event and map, with availability managed in coordination with the later order feature.
- **Producer**: An account with the `PRODUCER` permission, granted through the preceding Foundation and Security feature, and owner of its events.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A valid producer can publish an event with at least one offer, and the resulting event is available through the public catalog without an administrator approval step.
- **SC-002**: Every published event returned by the public catalog can be opened in a detail view containing the confirmed event fields and configured offers.
- **SC-003**: No buyer, unauthenticated client, or unrelated producer can create or modify an event through direct API requests.
- **SC-004**: Every published offer has a valid configured price and quantity, and invalid values are rejected before public publication.
- **SC-005**: For an assigned-seat map, no two saleable seats within the same event share an identity, each seat references one category, and its configured availability can be queried independently.
- **SC-006**: Updating event editorial data or current offer terms does not alter the recorded price or identity of previously sold tickets.

## Assumptions

- This feature depends on the implemented producer role and server-side authorization from [001-foundation-security](../001-foundation-security/spec.md).
- Marketplace launch remains Brazil-focused and web-responsive, as confirmed in the product interview.
- Public discovery initially consists of an event list and detail page; filters/search are not included based on the selected MVP option.
- Each category has one current price and quantity; there are no automatic sequential lots, discounts, or buyer purchase limits in this feature.
- Producers publish directly; no moderation/approval queue is introduced.
- Buyer checkout, payment processing, order-managed inventory decrement, ticket issuance, QR validation, refunds, and producer settlement are separate later features.
- Prices are BRL stored in cents, zero-price offers are allowed, images are HTTPS URLs, and events are physical with local date/time in `America/Sao_Paulo`.
- The map editor generates seats from sectors/rows; a category assigned per seat determines the seat's price.
- Admin can suspend or cancel; the producer owner can reactivate suspended events; cancelled events are terminal and retain a public detail notice.
- The current implementation includes all published events in the public list regardless of date and hides suspended-event details; confirm whether these are the intended long-term policies.
- **[NEEDS CLARIFICATION: maximum category quantity, exact age-classification values, past-event listing, suspended-event detail visibility, admin edit powers, and accessible-seat metadata are not specified].**