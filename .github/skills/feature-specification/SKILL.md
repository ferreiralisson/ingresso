---
name: feature-specification
description: 'Use when turning a backlog item, interview, or product request into a testable feature specification. Produces prioritized user journeys, requirements, edge cases, measurable outcomes, assumptions, and explicit clarification markers using the bundled template.'
argument-hint: 'Backlog item or feature request to specify'
---

# Feature Specification

Create or update a requirements-first feature specification from confirmed product input. Do not implement application code as part of this workflow unless separately requested.

## When to Use

- A user asks to specify, scope, or prepare a feature before implementation.
- A user asks to convert an interview, product backlog, or written request into acceptance criteria.
- A user asks for a Spec Driven Development artifact in the project's feature-spec format.

## Procedure

1. **Find the source of truth.** Read the referenced backlog, interview notes, existing architecture/product documents, and nearby application code needed to establish current behavior. Distinguish implemented behavior from proposed work.
2. **Choose the feature boundary.** Use the exact backlog item requested. Do not silently expand it to adjacent roadmap items. Identify dependencies and out-of-scope capabilities.
3. **Resolve material ambiguity.** Ask concise, decision-oriented questions when answers could change the user journey, business rule, access control, or data lifecycle. If the user wants a draft immediately or does not answer, record the missing decision as `[NEEDS CLARIFICATION: ...]`; do not invent a definitive rule.
4. **Use the bundled template.** Start from [feature-spec-template.md](./assets/feature-spec-template.md) and preserve its required sections. Save the specification in the repository's established spec location. If none exists, use `specs/<###-feature-name>/spec.md` and a sequential branch slug such as `001-foundation-security`.
5. **Write independently testable journeys.** Order user stories by priority (P1 first). For each story, state the actor and value, why it has that priority, an independent test, and Given/When/Then acceptance scenarios. A story should deliver demonstrable value without requiring every other story unless the dependency is explicit.
6. **Specify observable requirements.** Assign stable IDs (`FR-001`, etc.). Describe capabilities and outcomes, not implementation classes or framework-specific mechanics unless the input explicitly mandates them. Include roles, authorization boundaries, lifecycle, validation, errors, and persistence only when relevant to the feature.
7. **Cover edge cases.** Include malformed or missing data, unauthorized access, repeated requests, concurrency, failure/retry, expiry, migration, and boundary conditions appropriate to the feature.
8. **Define measurable success.** Use testable outcome measures tied to acceptance scenarios. Avoid unsupported scale, performance, or business targets; mark targets requiring product decisions as clarification items.
9. **Record assumptions and entities.** Include key data concepts without dictating implementation. Label assumptions explicitly and keep them consistent with requirements and scenarios.
10. **Check consistency.** Verify that every confirmed input appears in the appropriate requirement or scenario; pending items remain visibly unresolved; no feature is described as implemented unless code confirms it; internal links resolve; and the frontmatter/branch/date/status are populated.

## Clarification and Evidence Rules

- Treat code, tests, configuration, and explicit user answers as evidence; do not infer product behavior from names, artwork, or conventional marketplace patterns.
- Use `**[NEEDS CLARIFICATION: ...]**` or the template's inline form for decisions that materially affect scope or behavior.
- Separate approved requirements, assumptions, recommendations, dependencies, and out-of-scope items.
- If a requirement conflicts with another (for example, asynchronous payment approval versus stock guarantees), state the conflict and request a policy decision instead of choosing silently.
- Avoid requirements that cannot be tested. Convert each requirement into a scenario or measurable outcome where practical.
- Do not claim legal, privacy, financial, or production readiness without confirmed requirements and evidence.

## Output Checklist

- Feature name, branch slug, created date, Draft status, and source description are present.
- User stories have priorities, rationale, independent tests, and acceptance scenarios.
- Edge cases, functional requirements, key entities (when relevant), measurable success criteria, and assumptions are complete.
- Open questions are explicit and do not masquerade as decisions.
- The specification is linked to relevant architecture/backlog documents when those files exist.