# Feature Specification: Foundation and Security

**Feature Branch**: `001-foundation-security`

**Created**: 2026-10-06

**Status**: Implemented (event-scoped staff grants deferred to the event feature)

**Input**: User description: "Implementar a primeira etapa do plano de evolução: papéis e convites, autorização no servidor, configuração persistente por ambiente e migrações, removendo a possibilidade de cadastro público como ADMIN."

**Source material**: [Funcionalidades a Implementar](../../Funcionalidades%20a%20Implementar.md) and [Arquitetura do Sistema](../../Arquitetura%20do%20Sistema.md).

## User Scenarios & Testing

### User Story 1 - Public buyer registration stays least-privileged (Priority: P1)

As a visitor, I can create a buyer account, but cannot grant myself producer, staff, or administrator privileges through public registration data.

**Why this priority**: The previous public API mapped the client-supplied `perfil=ADMIN` to administrator access. Closing this privilege-escalation path is a prerequisite for safely adding producer roles.

**Independent Test**: Register through the public API with no role, with `USER`, and with attempted privileged role values. Verify the resulting account has buyer permissions only and cannot access administrator or producer operations.

**Acceptance Scenarios**:

1. **Given** a visitor submits valid public registration data without a role, **When** the account is created, **Then** it receives only the default buyer permissions.
2. **Given** a visitor submits a role field containing `ADMIN`, `PRODUCER`, or `STAFF`, **When** the request is processed, **Then** the legacy field is ignored and the account receives only `USER`.
3. **Given** a newly registered buyer calls an administrator- or producer-protected API operation, **When** the API evaluates the request, **Then** access is denied and no protected state changes.

### User Story 2 - Administrator invites a producer (Priority: P1)

As an administrator, I can invite a user to become a producer so that event-management access is granted only through an authorized administrative action.

**Why this priority**: The marketplace requires third-party producers, while producer access must not be self-assigned through public signup.

**Independent Test**: With an administrator and a buyer account available, create a producer invitation, accept it as the invited identity, and verify producer authorization. Also verify a different identity cannot accept it.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator, **When** they invite an eligible identity to become a producer, **Then** the system records an invitation bound to that identity and does not grant producer access before acceptance.
2. **Given** a valid, unused invitation, **When** the invited identity accepts it, **Then** producer permission is granted and the invitation cannot be used again.
3. **Given** a non-administrator, **When** they attempt to create or revoke a producer invitation, **Then** the API denies the operation.
4. **Given** an invitation that is invalid, already used, revoked, or older than 48 hours, **When** it is accepted, **Then** no producer permission is granted.
5. **Given** an administrator creates an invitation, **When** the API responds, **Then** the raw token is returned only in that response and must be delivered to the invitee outside the application.

### User Story 3 - API authorization is enforced by the server (Priority: P1)

As the system owner, I need protected operations to authorize the authenticated principal and its current permissions on the server, so browser navigation controls cannot be bypassed by direct API calls.

**Why this priority**: Angular guards and hidden UI controls are not security boundaries. Role checks must protect direct HTTP requests as the domain grows.

**Independent Test**: Call representative protected operations directly with no token, an invalid token, a buyer token, and an authorized administrator/producer identity. Verify the API permits only the documented action and resource scope.

**Acceptance Scenarios**:

1. **Given** a request to a protected operation without valid authentication, **When** the API authorizes it, **Then** the operation is not executed.
2. **Given** an authenticated buyer calls an administrator- or producer-only operation, **When** the API authorizes it, **Then** the operation is denied even if the caller bypasses the web application.
3. **Given** a producer without ADMIN permission calls the administrator-only user-list operation directly, **When** the API authorizes it, **Then** access is denied.
4. **Given** a producer permission is revoked, **When** the same JWT is used on the next request, **Then** authorities are reloaded from persistence and the revoked permission is no longer present.

### User Story 4 - Application configuration and schema are reproducible (Priority: P2)

As an operator, I can configure the application per environment and apply versioned database schema changes so account and authorization data are not lost when the process restarts.

**Why this priority**: The current API uses an in-memory H2 database and automatic schema updates. Persisted users, invitations, and future marketplace data require an explicit durable environment and controlled schema evolution.

**Independent Test**: Start the API against a configured persistent database, apply migrations to a fresh schema, create an account, restart the API, and verify the account remains available and can authenticate.

**Acceptance Scenarios**:

1. **Given** a fresh database and valid environment configuration, **When** the API starts, **Then** versioned migrations create the required schema and the application becomes ready.
2. **Given** a user account exists in the persistent database, **When** the API restarts, **Then** the account remains and can authenticate.
3. **Given** a required secret or database setting is missing or invalid, **When** the API starts, **Then** it fails safely with an actionable configuration error and does not fall back to a known production secret.
4. **Given** a schema migration has already been applied, **When** the application starts again, **Then** that migration is not applied a second time.

### Edge Cases

- A public registration request includes an unknown, malformed, or privileged role value.
- An invitation is accepted by an identity other than the invited identity, accepted twice, revoked before acceptance, or accepted after its 48-hour expiration.
- Two administrators attempt to invite the same identity, or an invitation targets an account that already has producer access.
- An administrator attempts to grant or revoke their own final administrator access.
- A role is revoked while a user still holds a previously issued JWT.
- A buyer directly calls a protected route without using the Angular application.
- Database configuration is missing, the database is unavailable, or a migration fails partway through startup.
- A migration is applied to a database containing existing buyer accounts; the migration must not silently delete or elevate those accounts.
- The first administrator cannot be created through public registration and must be manually provisioned in the database using the documented runbook.
- A legacy database contains ADMIN grants created through the formerly vulnerable public endpoint; migration demotes these to USER and records the change.
- Event-scoped entry-staff authorization cannot be exercised until the event model exists; it is deferred to that feature.

## Requirements

### Functional Requirements

- **FR-001**: System MUST assign only buyer permissions through public account registration, regardless of role fields supplied by the client.
- **FR-002**: System MUST prevent public users from granting themselves producer, staff, or administrator permissions.
- **FR-003**: System MUST allow an authenticated administrator to invite an identity to become a producer.
- **FR-004**: System MUST bind a producer invitation to its intended identity and MUST grant producer permission only after valid acceptance by that identity.
- **FR-005**: System MUST prevent an invitation from granting permission more than once and MUST reject invalid, revoked, or 48-hour-expired invitations.
- **FR-006**: System MUST authorize protected API operations on the server using the authenticated identity and its current permissions; frontend route guards MUST NOT be treated as authorization.
- **FR-007**: System MUST deny requests when the identity is unauthenticated or lacks permission for the operation.
- **FR-008**: System MUST defer event-scoped entry-staff grants and producer ownership checks until event resources exist; no global STAFF permission is created by this feature.
- **FR-009**: System MUST keep administrator privileges unavailable through public registration and document manual database provisioning of the initial ADMIN.
- **FR-010**: System MUST support an environment-configurable H2 file database and external JWT secret; `TOKEN_SECRET` MUST contain at least 64 UTF-8 bytes.
- **FR-011**: System MUST apply database schema changes through versioned Flyway migrations and configure Hibernate to validate rather than mutate the schema.
- **FR-012**: System MUST preserve existing user accounts and credentials through migration, while converting legacy ADMIN roles to USER and recording the demotion.
- **FR-013**: System MUST not use a source-controlled JWT secret or silently start without a sufficiently strong configured secret.
- **FR-014**: System MUST use H2 file storage and Flyway for this implementation; production database selection is outside this feature.
- **FR-015**: System MUST record who granted, accepted, revoked, or changed privileged access and when, so administrative role changes can be audited.
- **FR-016**: System MUST not expose an application endpoint to grant/revoke ADMIN; manual database administration MUST follow the runbook and preserve at least one usable administrator.

### Key Entities

- **Account**: An authenticated identity with existing account data and accumulated permissions; a producer retains USER.
- **Permission Grant**: An auditable assignment of a permission to an account, including grantor, target, time, and optional resource scope.
- **Invitation**: A 48-hour, single-use producer grant bound to an e-mail address, with hashed token, inviter, status, and acceptance/revocation timestamps. The invitee must authenticate as that e-mail; token delivery occurs outside the application.
- **Environment Configuration**: Database connection and secret settings scoped to the deployment environment; values must not be embedded as production defaults.
- **Schema Migration**: A versioned change to persistent application data structures, with recorded application status.
- **Event Scope**: A future resource boundary for producer ownership and entry-staff permissions; event-level grants are deferred until event management exists.

## Success Criteria

### Measurable Outcomes

- **SC-001**: In the public-registration test matrix, zero accounts gain producer, staff, or administrator permissions from client-supplied data.
- **SC-002**: All protected-operation authorization tests deny unauthenticated and insufficiently authorized requests, including requests sent directly to the API.
- **SC-003**: Every valid producer invitation can be accepted at most once by its intended identity; invalid, revoked, or expired invitations result in zero privilege grants.
- **SC-004**: A migrated account remains available after restart; legacy ADMIN grants become USER and are present in the audit table.
- **SC-005**: Re-running application startup after a successful migration does not duplicate or corrupt schema changes.
- **SC-006**: No production startup succeeds using the known sample JWT secret when the required secret has not been configured.
- **SC-007**: Every privileged permission change can be traced to an actor and timestamp.

## Assumptions

- Existing email/password login and JWT authentication remain the identity foundation; permissions are accumulated and loaded from persistence on every authenticated request.
- Public registration continues to create a buyer account, corresponding to the current `USER` profile.
- Server-side authorization is authoritative; Angular guards remain a user-experience mechanism.
- An administrator must exist before producer invitations can be sent; the first is created by an operator directly in the database using the runbook.
- A producer invitation is bound to a normalized e-mail, accepted by an authenticated account with that e-mail, and expires after 48 hours. The token is delivered out of band by the administrator.
- Event-scoped entry-staff invitations and producer ownership checks are deferred until the event model exists; this feature does not add event CRUD or an email sender.
- H2 file persistence is the selected MVP/local database; a production database is a later decision.
- Production secrets and database connection values will be supplied outside source-controlled application defaults.
- Payment processing, event management, ticket inventory, ticket issuance, QR validation, and refunds are outside this feature's scope.