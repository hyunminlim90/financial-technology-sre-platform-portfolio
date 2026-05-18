# Governance Timeline API Contract

## Purpose

This document defines the HTTP API surface contract for governance timeline query, pagination, health, and runtime endpoints.

Timeline APIs are read-only internal operational interfaces.

This phase defines contract only.

It does not introduce query implementation, health controllers, Micrometer recorders, SSE, WebSocket, or streaming execution.

## API Endpoints

Recommended timeline API surface:

- `GET /internal/governance/timeline`
- `GET /internal/governance/timeline/health`
- `GET /internal/governance/timeline/runtime-summary`

## Response Envelope

Recommended timeline response envelope:

```json
{
  "respondedAt": "2026-05-13T00:00:00Z",
  "status": "SUCCESS",
  "page": {
    "items": [],
    "page": {
      "nextCursor": "opaque-next-cursor",
      "previousCursor": "opaque-previous-cursor",
      "hasNext": true,
      "hasPrevious": false,
      "limit": 50,
      "direction": "NEXT",
      "ordering": "occurredAt DESC, eventId DESC",
      "degraded": false,
      "failedComponents": []
    }
  },
  "degradation": {
    "degraded": false,
    "partialTimeline": false,
    "mode": "STRICT",
    "failedComponents": [],
    "reason": "none"
  },
  "errors": []
}
```

## Query Parameters

Recommended timeline query parameters:

- `cursor`
- `limit`
- `direction`
- `from`
- `to`
- `eventType`
- `includeDegraded`

## Cursor Opaque Rule

Timeline cursors are opaque transport values.

Clients must not:

- parse cursor contents
- infer event IDs or record IDs from cursors
- modify cursor values
- expose cursor values in logs or metrics

## Degraded Response Semantics

Timeline APIs may return degraded but safe read-only responses.

When degraded:

- `status` may be `DEGRADED`
- `degradation.degraded` must be `true`
- `failedComponents` should be included when available
- successful events may still be returned
- clients must mark the timeline as partial

## Operator-safe Error Semantics

Timeline API errors must be safe for operator-facing UI rendering.

Recommended error shape:

- `code`
- `message`

Errors must not expose:

- exception messages
- stack traces
- SQL errors
- DB internals
- cursor contents
- raw payloads

## Read-only Guarantees

Timeline APIs must not:

- trigger remediation
- approve recommendations
- execute plans
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- update RAG
- update Qdrant

## Internal-only Contract

Timeline APIs are internal-only.

They must not be exposed as public product APIs or customer-facing APIs.

## Future Streaming Compatibility

The timeline API surface should remain compatible with future:

- SSE
- WebSocket
- WebFlux streaming
- incremental polling

This contract does not introduce streaming implementation.

## Non-goals

This contract does not introduce:

- timeline query implementation
- DB pagination implementation
- health controller implementation
- runtime controller implementation
- Micrometer recorder implementation
- SSE
- WebSocket
- mutation APIs
