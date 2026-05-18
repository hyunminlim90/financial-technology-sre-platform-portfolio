# Governance Timeline Frontend Integration Contract

## Purpose

This document defines how the React SRE Console should render and consume governance timeline data.

Governance timeline is a read-only append-only operational audit stream.

This contract does not introduce timeline mutation, remediation actions, approval execution, GitOps mutation, RAG ingestion, Qdrant updates, SSE, WebSocket, or frontend implementation.

## Timeline Panel Usage

The timeline panel should be used in:

- incident detail pages
- recommendation detail pages
- learning detail pages
- knowledge update detail pages
- future cross-incident governance views

Timeline events must be rendered as read-only audit entries.

## Cursor Pagination UX

The frontend must treat timeline cursors as opaque values.

Allowed behavior:

- pass `nextCursor` to fetch the next page
- pass `previousCursor` to fetch the previous page
- preserve filters while paginating
- preserve `limit`
- preserve `direction`

Forbidden behavior:

- parse cursor contents
- infer record IDs from cursors
- modify cursor values
- use cursor values as metric tags
- log cursor values as user-visible diagnostics

## Infinite Scroll Rules

Timeline infinite scroll should:

- request the initial page with no cursor
- use `nextCursor` for older events
- avoid offset pagination
- avoid aggressive polling during active incidents
- deduplicate events by `eventId` on the client if needed
- preserve stable ordering by `occurredAt DESC, eventId DESC`

## Degraded Timeline Rendering

If timeline metadata contains `degraded=true`, the UI must:

- show a partial timeline badge
- display failed source components
- display the low-cardinality reason
- clearly mark the timeline as incomplete
- keep successful events visible
- avoid presenting partial timelines as complete

Degraded timeline data is safe to render but must be disclosed.

## Event Severity Rendering

Recommended severity rendering:

| Severity | UI Treatment |
|---|---|
| `INFO` | Normal audit event |
| `WARNING` | Review-needed or degraded event |
| `ERROR` | Failed, rejected, or blocked governance event |
| `CRITICAL` | Security or payment integrity critical signal |

Severity must not trigger automatic actions.

## Actor and Resource Rendering

Timeline events may include:

- actor
- resource
- eventType
- severity
- occurredAt
- title
- summary

The UI should render actor or resource information as context, not as executable controls.

Actor or resource IDs must not be used as metric tags.

## Event Click Behavior

Clicking a timeline event may navigate to:

- lightweight overview preview
- full governance detail page
- related incident, recommendation, learning, or knowledge update detail

Clicking a timeline event must not execute operational actions.

## Incremental Polling Compatibility

The frontend may support incremental polling.

Recommended behavior:

- poll newer events only when the timeline panel is visible
- avoid polling full historical pages repeatedly
- merge new events by `eventId`
- keep cursor semantics stable
- show newly appended events without reordering older confirmed pages unexpectedly

## Future Streaming Compatibility

The timeline contract should remain compatible with future:

- WebFlux streaming
- SSE-based live incident timeline
- incremental event feed
- operator live-view mode

This contract does not introduce streaming implementation.

## UI States

### Loading

Render timeline skeleton rows.

### Empty

Show:

```text
No governance timeline events found.
```

### Degraded

Show partial timeline disclosure.

### Error

Show a read-only error state.

The UI must not offer remediation buttons from error states.

## Read-only Guarantees

The timeline UI must not expose buttons or workflows that:

- approve recommendations
- execute plans
- trigger remediation
- restart services
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- create Git commits or pull requests
- update RAG
- update Qdrant

## Sensitive Data Policy

The timeline UI must not display or log:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts
- cursor values
- exception messages

## Internal-only Assumptions

Governance timeline views are internal operational UI views.

They must not be exposed as public product APIs or public customer-facing pages.

## Related Contracts

- `docs/governance-timeline-pagination-contract.md`
- `docs/governance-timeline-query-contract.md`
- `docs/governance-timeline-read-model-contract.md`
- `docs/governance-timeline-mapping-contract.md`
- `docs/governance-timeline-aggregation-contract.md`
- `docs/governance-timeline-resilience-contract.md`
- `docs/governance-timeline-metrics-contract.md`
- `docs/governance-timeline-health-contract.md`
- `docs/governance-timeline-runtime-contract.md`
