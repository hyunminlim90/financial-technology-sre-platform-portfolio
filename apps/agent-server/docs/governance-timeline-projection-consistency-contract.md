# Governance Timeline Projection Consistency Contract

## 1. Projection Consistency Goals

This document defines future consistency semantics for Governance Timeline
projection persistence and projection-backed query behavior.

Projection consistency goals include:

- stable timeline ordering
- stable cursor semantics
- best-effort append-only consistency
- operator-facing consistency visibility

## 2. Ordering Consistency Guarantees

Projection-backed timeline behavior must preserve:

- `occurredAt DESC, eventId DESC` consistency
- stable pagination consistency
- minimized ordering drift

Ordering drift may occur during rebuild, replay, or degraded source conditions,
but it should be minimized and must not invalidate the core Timeline ordering
contract.

## 3. Cursor Consistency Guarantees

Projection-backed cursor behavior must preserve:

- opaque cursor compatibility
- cursor ordering compatibility
- cursor replay compatibility
- minimized cursor drift

Cursor transport shape may evolve internally, but operator-facing cursor
semantics must remain stable.

## 4. Deduplication Consistency Guarantees

Projection consistency must preserve:

- `event_id` uniqueness
- minimized duplicate projection visibility
- idempotent projection compatibility

Duplicate projection rows should not become normal operator-visible behavior.

## 5. Replay Consistency Guarantees

Replay and rebuild behavior must preserve:

- replay and rebuild ordering consistency
- idempotent replay consistency
- historical rebuild consistency

Replay should restore a projection-backed timeline that remains compatible with
the same ordering and pagination expectations used before replay.

## 6. Retention Consistency Guarantees

Retention and archive behavior must preserve:

- archive and replay coexistence consistency
- minimized retention-induced pagination inconsistency
- maximum cursor compatibility after retention

Retention is allowed to change active storage layout, but not to break the
external Timeline contract for supported active ranges.

## 7. Degraded Consistency Semantics

Projection consistency may degrade under partial failure.

The following semantics must remain supported:

- partial degraded projection still preserves best-effort consistency
- failed source isolation remains visible
- consistency degradation visibility remains available

Degraded consistency does not imply silent corruption tolerance. It implies
explicit, operator-visible best-effort behavior.

## 8. Operator-facing Consistency Visibility

Consistency observability is operator-facing informational semantics only.

Consistency visibility must not imply:

- auto-remediation semantics
- governance action trigger semantics

Consistency signals are intended to help operators reason about read-model
health, drift, replay state, and degraded visibility without triggering unsafe
actions.

## 9. Migration Expectations

Expected future migration behavior:

- runtime aggregation to persistent projection migration compatibility remains
  supported
- frontend and API ordering compatibility must remain preserved
- cursor contract must remain preserved
- projection replay compatibility must remain preserved

Internal storage and recovery strategy may evolve, but externally visible
Timeline consistency semantics should remain stable.

## 10. Non-goals

This contract does not introduce:

- strict distributed transaction guarantee
- exactly-once global ordering
- cross-region total ordering
- event sourcing migration
- distributed lock orchestration
