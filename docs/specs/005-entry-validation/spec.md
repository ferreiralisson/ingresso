# Feature Specification: Event Entry Validation

**Feature Branch**: `005-entry-validation`

**Created**: 2026-10-07

**Status**: Implemented

**Input**: User request to implement item 5 in the recommended sequence in [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md). Confirmed decisions: camera plus manual-code fallback; offline validation with synchronization; event-scoped staff; ADMIN correction reopens a ticket once; validation only on the event's local date while published; invitations expire after 48 hours; offline manifest lease is 24 hours; multiple offline accepts resolve by first server synchronization.

**Source material**: [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md), [Arquitetura do Sistema](../../Arquitetura%20do%20Sistema.md), [Ticket Delivery](../004-ticket-delivery/spec.md), [Foundation and Security](../001-foundation-security/spec.md), and [Events and Offers](../002-events-and-offers/spec.md).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Operator validates an admission (Priority: P1)

As an authorized producer, event staff member, or administrator, I can scan or enter a ticket QR for an event so that the server confirms whether the ticket can be admitted and prevents a second online use.

**Why this priority**: Ticket QR codes have no admission value until event staff can verify and consume them. Server-side authorization and one-time use are the core operational controls.

**Independent Test**: Create a paid order and issued ticket, authorize an operator for its event, validate the QR once, then submit the same QR again. Verify the first result is accepted, the second reports prior use, and a concurrent duplicate cannot be accepted online.

**Acceptance Scenarios**:

1. **Given** an authorized operator and an issued, unused ticket for the selected event, **When** the operator scans its QR or enters its opaque code while online, **Then** the system accepts the admission and records the ticket as used.
2. **Given** a ticket has already been used, **When** another online scan is submitted, **Then** entry is refused and the operator sees that it was previously used, including the recorded time and operator when available.
3. **Given** a QR is malformed, unknown, not issued, unpaid, or belongs to another event, **When** it is checked, **Then** it is rejected without marking a ticket as used.
4. **Given** two operators validate the same unused ticket concurrently while online, **When** both requests reach the system, **Then** at most one admission is accepted.
5. **Given** camera permission is denied or a camera is unavailable, **When** the operator uses the manual fallback, **Then** the same server-side validation rules apply.

### User Story 2 - Producer delegates event-scoped entry access (Priority: P1)

As the producer responsible for an event, I can invite and revoke entry staff for selected events so the team can validate only admissions they are assigned to handle.

**Why this priority**: Event operations commonly require staff who are not producers; their access must be limited to the relevant event and must not become a global role.

**Independent Test**: Have a producer invite a staff identity for one event, accept the invitation as that identity, and verify entry access for that event only. Verify an unrelated event, an unaccepted invite, a revoked grant, and a non-owner producer are denied.

**Acceptance Scenarios**:

1. **Given** a producer owns an event, **When** they invite a staff identity for that event, **Then** no entry permission is active before the intended identity accepts while authenticated.
2. **Given** an accepted event-scoped staff grant, **When** the staff member validates a ticket for the assigned event, **Then** access is permitted without granting producer or global staff permissions.
3. **Given** the same staff member attempts to validate a ticket for an event without an active grant, **When** the server authorizes the operation, **Then** it is denied.
4. **Given** a producer attempts to invite staff to an event they do not own, **When** the request is submitted, **Then** it is denied and no grant is created.
5. **Given** an event-scoped grant is revoked, **When** the staff member next validates online, **Then** access is denied using the current server-side grant state.
6. **Given** an administrator validates an event ticket, **When** authorization runs, **Then** the administrator may operate globally and the action is audited.

### User Story 3 - Operator continues entry work offline and synchronizes (Priority: P1)

As an authorized operator, I can provision an event for offline use, record provisional scans without network access, and synchronize them later so entry operations can continue during connectivity loss.

**Why this priority**: Offline operation was explicitly selected for the event-entry flow. The system must make provisional outcomes and cross-device conflicts visible rather than presenting offline scans as globally confirmed.

**Independent Test**: Synchronize event validation data to two authorized devices, disconnect them, and scan the same ticket on both. Verify each local result is marked provisional, the first scan batch synchronized by the server is accepted, and the later one is flagged as a duplicate conflict for review.

**Acceptance Scenarios**:

1. **Given** an authorized device has current offline validation data for an event, **When** network connectivity is lost and a locally valid unused QR is scanned, **Then** the device records a provisional admission and clearly indicates it has not been confirmed by the server.
2. **Given** a QR is not in the device's offline validation data, **When** it is scanned offline, **Then** the device does not report a confirmed admission and indicates that online verification is required.
3. **Given** the same ticket is provisionally accepted on more than one offline device, **When** records synchronize, **Then** the first scan record received by the server is accepted and later records are marked as duplicate conflicts; the system does not claim it prevented the already possible physical admissions.
4. **Given** an offline scan batch is retried after a timeout, **When** it is submitted again, **Then** the same scan records are returned or acknowledged without creating duplicate audit entries.
5. **Given** an offline record cannot be reconciled with the current server event/ticket state, **When** synchronization completes, **Then** it is reported as a conflict and is not represented as a confirmed server-side admission.

### User Story 4 - Administrator audits and corrects an entry record (Priority: P2)

As an administrator, I can review entry attempts and correct a mistaken check-in with a reason so operational records remain explainable.

**Why this priority**: Duplicate or mistaken scans need an accountable resolution path, especially when offline conflicts arrive later.

**Independent Test**: Create accepted, duplicate, and offline-conflict records; verify an administrator can review their event, ticket, operator, and timestamps and make a correction that records the actor and reason. Verify producers and entry staff cannot perform the correction.

**Acceptance Scenarios**:

1. **Given** validation records exist for an event, **When** an administrator reviews the audit, **Then** records include the event, ticket, operator, outcome, and relevant scan/synchronization timestamps.
2. **Given** an administrator corrects a mistaken check-in, **When** the correction is submitted with a reason, **Then** the original record is retained and the correction records the administrator and time.
3. **Given** a producer or event staff member attempts the same correction, **When** authorization is evaluated, **Then** the operation is denied and the audit remains unchanged.

### Edge Cases

- A ticket is already used, was never issued, belongs to another event, or its order is not paid.
- QR input is empty, malformed, truncated, or contains a token for a deleted/unknown ticket.
- Two online scans race for the same ticket; only one may consume it.
- Camera permission is denied, camera hardware is missing, or the camera cannot focus; manual entry must remain available.
- An operator loses connectivity during a scan or during synchronization and retries the same batch.
- Two or more offline devices provisionally accept the same ticket; physical admissions may occur before the first-sync-wins conflict is known.
- The offline event roster or operator grant becomes stale because a ticket/event/grant changes while a device is disconnected.
- A producer tries to assign staff to another producer's event or a staff member changes the event identifier in a request.
- An administrator correction is repeated, lacks a reason, or targets a record that has already been corrected.
- An event grant is revoked while a device is offline; scans remain provisional locally and the server rejects the batch after rechecking current authorization.
- A ticket scanned during a valid 24-hour offline lease is synchronized after that lease expires; the server may accept the scan if its device timestamp was inside the lease and current event/ticket checks pass.
- An ADMIN correction reopens the ticket once; a second correction for the same ticket is refused.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST require an authenticated operator for online validation, event provisioning, synchronization, audit access, and corrections.
- **FR-002**: System MUST authorize a producer only for events they own, event staff only for events with an active event-scoped grant, and ADMIN globally.
- **FR-003**: Entry staff grants MUST be scoped to one or more selected events, MUST be accepted by the invited authenticated identity, and MUST be revocable by an authorized producer or administrator. They MUST NOT grant global producer or staff authority.
- **FR-004**: System MUST validate QR identifiers against issued tickets on the server; client-provided ticket status, event identity, or use state MUST NOT be trusted.
- **FR-005**: Online validation MUST accept an eligible unused ticket at most once, including when concurrent requests target the same ticket.
- **FR-006**: System MUST reject malformed, unknown, unpaid/unissued, refunded, previously consumed, or wrong-event tickets without creating a successful check-in.
- **FR-007**: A repeat scan MUST return an already-used result without accepting another admission and MUST identify the prior use time/operator when available.
- **FR-008**: The web scanner MUST support camera scanning and manual entry of the QR value, with the same authorization and validation rules for both paths.
- **FR-009**: System MUST record validation outcomes with the event, ticket when known, operator, outcome, and server timestamp; offline-origin records MUST also preserve the device scan timestamp and synchronization timestamp.
- **FR-010**: ADMIN MUST be able to correct an entry record with a required reason; the original record and correction actor/time MUST remain auditable.
- **FR-011**: An authorized operator MUST be able to provision offline validation data for assigned events before connectivity is lost.
- **FR-012**: Offline scans MUST be visibly provisional until the server synchronizes and confirms them; an unknown QR not present in the offline data MUST NOT be shown as confirmed.
- **FR-013**: Synchronizing the same offline scan record more than once MUST be idempotent.
- **FR-014**: If the same ticket is provisionally accepted by multiple offline devices, the first scan record received by the server MUST be accepted and later records MUST be flagged as duplicate conflicts for review. The system MUST disclose that offline mode cannot prevent multiple physical admissions before synchronization.
- **FR-015**: Synchronization MUST reject or flag records that are unauthorized, malformed, for a different event, or incompatible with the current ticket state; they MUST NOT be silently reported as confirmed.
- **FR-016**: Offline event data and the local operator lease MUST expire 24 hours after provisioning. The lease MUST be limited to its provisioned event and operator; after JWT expiry, scans remain provisional and synchronization requires a fresh authenticated session.
- **FR-017**: Online check-in MUST be allowed only while the event is `PUBLISHED` and on its local calendar date in `America/Sao_Paulo`; offline scans MUST be checked against that event date and current event status during synchronization.
- **FR-018**: Offline manifests and pending scans MUST store only SHA-256 QR token hashes, not raw QR bearer values. Synchronization MUST revalidate the operator, event, and ticket on the server.
- **FR-019**: An ADMIN correction MUST reopen an accepted ticket once, retain the original check-in, record a required reason and administrator/time, and prevent a second correction for that ticket.
- **FR-020**: If an offline batch belongs to an operator whose event grant was revoked before synchronization, the server MUST persist a rejected `OPERATOR_UNAUTHORIZED` outcome for each scan rather than confirm entry.

### Key Entities *(include if feature involves data)*

- **Entry Staff Grant**: An authorization connecting a staff identity, producer, and one or more events, with invitation/acceptance/revocation lifecycle and grant audit information.
- **Check-in Record**: An immutable account of a validation attempt, including event, ticket, operator, outcome, server time, and optional device scan/synchronization times.
- **Offline Scan Batch**: A set of locally recorded validation attempts from an authorized operator/device, with unique record identities and synchronization outcomes.
- **Check-in Correction**: An administrator's reasoned amendment to a prior entry record, linked to the original record without deleting its audit history.
- **Entry Operator**: A producer who owns the event, an authenticated staff identity with an active event-scoped grant, or an administrator.
- **Issued Ticket**: The opaque QR-backed ticket from [004-ticket-delivery](../004-ticket-delivery/spec.md), whose authoritative validity/use state is determined by the server after synchronization.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In concurrent online validation tests for one unused ticket, exactly one request is accepted and all others receive an already-used result.
- **SC-002**: Unauthorized, unaccepted, revoked, wrong-event, and non-owner operators create zero successful check-ins.
- **SC-003**: Camera and manual-code paths produce the same server validation outcome for identical QR values.
- **SC-004**: ADMIN can review every accepted, rejected, duplicate, and corrected record with event, operator, outcome, source, and timestamps; corrections preserve the original record, reason, actor, and time.
- **SC-005**: Replaying an offline batch produces no duplicate server records; the first synchronized provisional scan wins and later duplicate scans are visibly flagged.
- **SC-006**: No UI presents an offline provisional result as server-confirmed before synchronization.

## Assumptions

- This feature depends on event ownership/status from [002-events-and-offers](../002-events-and-offers/spec.md), authentication/authorization from [001-foundation-security](../001-foundation-security/spec.md), and issued opaque QR tickets from [004-ticket-delivery](../004-ticket-delivery/spec.md).
- Existing system evidence includes only global `ADMIN`, global `PRODUCER`, and buyer `USER`; event-scoped entry access is a new grant concept, not a global role.
- The producer issues one 48-hour invitation per owned event; invitees authenticate as the invited identity and accept before online access is active. A staff identity may accept separate invitations for multiple events.
- Offline operation requires authenticated provisioning while connected. Its 24-hour lease is bound to the operator/event; the entry route can continue provisional operation after JWT expiry, but synchronization requires authentication again.
- Offline manifests and pending scans store QR SHA-256 hashes rather than raw QR bearer values. The server rechecks current authorization, event status/date, and ticket use during synchronization.
- For cross-device offline duplicates, first server synchronization wins as explicitly selected; this resolves data state but cannot undo multiple physical admissions that may occur before sync.
- ADMIN corrections require a reason, preserve the original audit row, and reopen a ticket once.
- Entry is allowed only on the event's local calendar date in `America/Sao_Paulo`, while event status is `PUBLISHED`; event status is checked again during sync.
- Camera/manual scanning is in scope; camera availability depends on browser permission and device support. The production PWA service worker caches the application shell.
- Payment provider integration, refunds, ticket transfers, and real-time revocation while disconnected are outside this feature or require separate policy decisions.