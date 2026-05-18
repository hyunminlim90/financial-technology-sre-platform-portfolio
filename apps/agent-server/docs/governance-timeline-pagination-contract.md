# Governance Timeline Pagination Contract

## Purpose

This document defines cursor pagination semantics for governance timelines.

Governance timelines are append-only merged event streams composed from recommendation, approval, execution, verification, incident lifecycle, postmortem, learning, and knowledge update records.

This phase defines contract only.

It does not introduce DB cursor queries, timeline streaming, SSE, WebSocket, or frontend implementation.

## Timeline Ordering

Recommended stable ordering:

```text
occurredAt DESC, eventId DESC
```

For equal timestamps, `eventId` must be used as a deterministic tie-breaker.

The frontend must not assume offset-based ordering.

## Cursor Identity

A timeline cursor should be based on:

- `occurredAt`
- `eventType`
- `eventId`

Cursor values exposed to the frontend must be opaque.

The frontend must not parse, modify, or infer cursor contents.

## Merged Event Stream

A governance timeline may merge events from:

- recommendation records
- approval records
- execution plans
- human execution results
- verification results
- incident lifecycle transitions
- postmortem drafts
- postmortem reviews
- learning candidates
- knowledge promotion reviews
- knowledge promotion plans
- knowledge update applications

The timeline is a read-only operational audit view.

## Consistency Semantics

Timeline pagination is best-effort consistent under concurrent append-only inserts.

The implementation should prefer:

```text
duplicate avoidance > perfect snapshot isolation
```

A newly appended event may appear on a newer page or after refresh.

The UI should tolerate minor ordering movement during active incidents.

## Partial Degraded Timeline

Timeline responses may be degraded if one or more source components fail.

When degraded, the response should include:

- `degraded=true`
- `failedComponents`

The UI must clearly mark the timeline as partial.

## Future Streaming Compatibility

The cursor contract should remain compatible with future:

- incremental polling
- WebFlux streaming
- SSE
- live incident timeline updates

This document does not introduce streaming in the current phase.

## Query Parameters

Recommended future parameters:

| Parameter | Meaning |
|---|---|
| `cursor` | Opaque timeline cursor |
| `limit` | Page size |
| `direction` | `NEXT` or `PREVIOUS` |
| `from` / `to` | Optional time range |
| `eventType` | Optional event type filter |

Rules:

- `limit` must be clamped server-side.
- cursor is opaque.
- filters must remain stable across pagination requests.
- changing filters invalidates existing cursors.

## Security and Sensitive Data

Timeline cursor and timeline items must not expose:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts

Timeline events must remain sanitized read-only audit entries.

## Non-goals

This contract does not introduce:

- actual DB cursor queries
- offset pagination
- streaming timeline
- SSE
- WebSocket
- frontend implementation
- mutation APIs
- remediation actions
- GitOps mutation
- RAG ingestion
- Qdrant updates
