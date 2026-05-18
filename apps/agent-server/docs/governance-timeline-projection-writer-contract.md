# Governance Timeline Projection Writer Contract

## 1. Projection Writer Goals

This document defines the future contract for a Governance Timeline projection
writer.

The goal is to preserve append-only audit semantics while allowing projected
timeline events to be persisted into a future projection store without changing
the external Timeline API contract.

## 2. Source-to-Projection Flow

Recommended future flow:

```text
source record
→ projection mapper
→ sanitization
→ projection writer
→ projection store
```

The projection writer sits after mapping and sanitization, and before the
persistent projection store.

## 3. Projection Write Semantics

Projection writes should preserve:

- append-oriented projection write behavior
- historical audit mutation prohibition
- `event_id` uniqueness
- minimal projection overwrite behavior

Projection persistence must prefer appending stable audit rows rather than
rewriting previously exposed audit history.

## 4. Idempotency Expectations

Projection writer behavior should remain idempotent at the event level.

Required expectations:

- `event_id`-based idempotent write behavior
- duplicate projection row creation is not allowed
- retry-safe write behavior is supported

Repeated delivery of the same logical projected event should not create a new
duplicate row.

## 5. Ordering Expectations

Projection writer behavior must preserve downstream query compatibility for:

- `occurredAt DESC, eventId DESC` ordering
- cursor ordering compatibility
- stable pagination compatibility

Write-path evolution must not break the ordering contract consumed by timeline
queries and frontend pagination.

## 6. Sanitization Boundary

Projection writer inputs must be sanitized before persistence.

Required boundary:

- sanitization is mandatory before projection write
- secrets, tokens, and passwords must not be stored
- payment data and customer PII must not be stored
- raw prompts and raw LLM responses must not be stored
- stack traces and raw logs must not be stored

Projection persistence must not become a backdoor for unsafe raw source
content.

## 7. Degraded Write Semantics

Projection write flows may degrade under partial failure.

Supported semantics:

- partial projection write failure is allowed
- best-effort degraded projection is preserved
- failed projection source isolation remains visible
- external exception detail exposure is not allowed

Projection write degradation must not silently erase successful projected
events that can still be written safely.

## 8. Retry Expectations

Projection write retry behavior should support:

- idempotent retry
- duplicate event prevention
- at-least-once retry compatibility

Exactly-once distributed delivery is not required for this contract, as long as
duplicate projection rows are still prevented by idempotent event handling.

## 9. Metrics Expectations

Future projection writers should expose observability such as:

- `projection_write_total`
- `projection_write_failure_total`
- `projection_write_degraded_total`

Metrics must preserve low-cardinality tag discipline.

Metric tags must not include:

- `eventId`
- raw error detail
- raw exception messages
- user-visible summary text

## 10. Migration Expectations

Expected future migration path:

- runtime aggregation may migrate toward projection persistence
- API compatibility must remain stable
- frontend cursor compatibility must remain stable
- degraded read semantics must remain stable

Internal persistence strategy may change, but externally visible Timeline
behavior must remain consistent.

## 11. Non-goals

This contract does not introduce:

- actual projection writer implementation
- R2DBC write repository
- Kafka pipeline
- CDC
- Debezium
- exactly-once distributed guarantee
- event sourcing migration
- SSE
- WebSocket
