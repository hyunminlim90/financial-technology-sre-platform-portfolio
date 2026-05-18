# Governance Console Cursor Pagination Contract

## Purpose

This document defines cursor pagination semantics for append-only governance records.

Cursor pagination is intended for:

- detail timelines
- search results
- backlog queues
- large governance history views
- future streaming-friendly UI patterns

This phase defines contract only.

It does not introduce DB cursor queries, reactive streaming, SSE, WebSocket, or frontend implementation.

## Cursor Principles

Cursors are opaque values.

The frontend must not parse, modify, or infer cursor contents.

Allowed frontend behavior:

- pass `nextCursor` as `cursor` for the next page
- pass `previousCursor` as `cursor` for the previous page
- preserve `limit`
- preserve filters and time range

## Stable Ordering

Governance records are append-only.

Recommended ordering:

```text
occurredAt DESC, recordId DESC
```

or for domain-specific records:

```text
createdAt DESC, recordId DESC
verifiedAt DESC, recordId DESC
appliedAt DESC, recordId DESC
```

A stable tie-breaker is required because multiple records may share the same timestamp.

## Offset Pagination

Offset pagination is discouraged for append-only governance records.

Reasons:

- unstable under concurrent inserts
- expensive for large datasets
- may skip or duplicate items during live operations
- unsuitable for incident timelines and audit history

## Query Parameters

Recommended parameters:

| Parameter | Meaning |
|---|---|
| `cursor` | Opaque cursor from previous response |
| `limit` | Page size |
| `direction` | `NEXT` or `PREVIOUS` |
| `window` | Optional dashboard or search time window |
| `from` / `to` | Optional custom time range |

Rules:

- `limit` should be clamped server-side.
- cursors must be treated as opaque.
- filters must remain stable across pagination requests.
- changing filters invalidates existing cursors.

## Response Shape

Recommended response shape:

```json
{
  "items": [],
  "page": {
    "nextCursor": "opaque-next-cursor",
    "previousCursor": "opaque-previous-cursor",
    "hasNext": true,
    "hasPrevious": false,
    "limit": 50,
    "direction": "NEXT",
    "ordering": "occurredAt DESC, recordId DESC"
  }
}
```

## Future Target APIs

Cursor pagination may be applied to:

- `/internal/governance/search`
- `/internal/governance/dashboard/backlog`
- `/internal/governance/details/incidents/{incidentId}`
- `/internal/governance/details/recommendations/{recommendationRecordId}`
- `/internal/governance/details/learning-candidates/{learningCandidateId}`
- `/internal/governance/details/knowledge-updates/{knowledgeUpdateApplicationId}`

The first implementation target should be detail timeline pagination.

## Security and Sensitive Data

Cursor values must not expose:

- raw record IDs in parseable form
- payment payloads
- customer data
- secrets
- tokens
- raw logs
- prompts

Cursor values should be encoded and treated as transport-only tokens.

## Non-goals

This contract does not introduce:

- actual DB pagination queries
- offset pagination
- streaming timeline
- SSE
- WebSocket
- frontend implementation
- mutation APIs
- remediation actions
