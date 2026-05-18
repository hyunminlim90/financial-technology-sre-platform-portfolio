# Governance Timeline Resilience Contract

## Purpose

This document defines degraded, partial, and fail-open semantics for governance timeline query and aggregation flows.

Governance timeline is a read-only operational audit view.

This phase defines contract only.

It does not introduce actual timeout operators, fallback queries, circuit breakers, SSE, WebSocket, or streaming implementation.

## Resilience Modes

| Mode | Meaning |
|---|---|
| `STRICT` | Any required component failure may fail the timeline response |
| `PARTIAL_DEGRADED` | Successful components may still be returned with degraded metadata |
| `FAIL_OPEN_READ_ONLY` | Timeline response may remain available with partial read-only data |

## Degraded Timeline Semantics

When a timeline is degraded:

- successful projections should still be returned
- failed source components should be listed
- the response must mark `degraded=true`
- the UI must mark the timeline as partial
- failed components must not be silently hidden
- no remediation or mutation is triggered

## Failure Reason Taxonomy

Allowed low-cardinality reasons:

- `component_query_failed`
- `component_query_timeout`
- `projection_failed`
- `aggregation_degraded`
- `timeline_query_timeout`

Responses must not expose exception messages, stack traces, SQL errors, or DB internals.

## Component Failure Contract

Each failed component should include:

- source
- reason

Example:

```json
{
  "source": "VERIFICATION",
  "reason": "component_query_timeout"
}
```

Source names must come from `GovernanceTimelineAggregationSource`.

## Append-only and Read-only Guarantees

Timeline resilience must not mutate source records.

Partial failure must not modify existing timeline events.

Degraded timeline responses are read-only audit views.

Timeline degradation must never trigger:

- remediation
- approval execution
- plan execution
- Kubernetes mutation
- ArgoCD mutation
- GitOps mutation
- RAG ingestion
- Qdrant updates

## Streaming Compatibility

The resilience contract should remain compatible with future:

- cursor pagination
- incremental polling
- WebFlux streaming
- SSE-based timeline updates

A stream may emit degraded metadata, but must not emit unsafe operational actions.

## Sensitive Data

Timeline degradation metadata must not expose:

- exception messages
- SQL errors
- stack traces
- payment payloads
- customer data
- secrets
- tokens
- raw logs
- full prompts

## Non-goals

This contract does not introduce:

- actual timeout implementation
- fallback query implementation
- circuit breaker
- retry policy
- DB query optimization
- projection table
- Kafka
- SSE
- WebSocket
- frontend implementation
