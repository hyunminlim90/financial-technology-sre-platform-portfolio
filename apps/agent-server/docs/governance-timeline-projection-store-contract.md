# Governance Timeline Projection Store Contract

## 1. Projection Store Goals

This document defines the future contract for a materialized Governance
Timeline projection store.

The goal is to preserve the current operator-facing Timeline API while allowing
the underlying timeline build path to evolve from runtime aggregation to a
materialized, read-optimized projection model.

## 2. Current Runtime Aggregation Characteristics

The current Governance Timeline implementation is centered on runtime
aggregation.

Current characteristics:

- governance source records are merged at query time
- timeline event projection happens inside the application runtime
- read performance depends on multi-source fan-out and merge cost
- the current model is contract-correct but not the final large-scale read path

This runtime aggregation model is suitable for current operator usage, but it
is not the only intended long-term storage strategy.

## 3. Future Projection Store Direction

Future direction may include:

- materialized timeline projection storage
- append-only event projection rows
- read-optimized query paths
- projection stores optimized for timeline pagination and operator history
- future PostgreSQL or similar persistent read-model implementations

Any future projection store must preserve the externally visible Timeline API
and cursor contract.

## 4. Projection Consistency Guarantees

Future projection stores must preserve:

- deterministic event projection semantics
- `eventId` uniqueness
- stable source-to-event mapping outcomes
- append-only historical audit behavior

The same logical governance source input should continue to produce the same
logical timeline event representation.

## 5. Projection Ordering Guarantees

Projection-backed query paths must preserve:

- `occurredAt DESC, eventId DESC` ordering
- opaque cursor stability
- `NEXT` semantics for older events
- `PREVIOUS` semantics for newer events
- stable pagination behavior across projection-backed pages

Changing the internal storage layout must not change ordering guarantees
observed by operators or frontend consumers.

## 6. Projection Update Semantics

Projection store updates must preserve append-only audit semantics.

This means:

- historical audit events must not be mutated in place
- new context should be represented as newly appended projection entries
- projection backfill or repair must not change operator-visible history shape
  in unsafe ways

Historical audit mutation is not allowed as part of the query model.

## 7. Degraded Projection Semantics

Projection paths may still degrade when source projection inputs partially
fail.

The following semantics must remain supported:

- partial projection availability is allowed
- best-effort degraded read availability is preserved
- projection source failures may be isolated and disclosed
- successful projected events may still be returned
- exception details must not be exposed

Projection materialization must not remove degraded read-only disclosure.

## 8. Read-only Query Guarantees

Projection-backed timeline querying remains:

- read-only
- append-only in audit semantics
- non-mutating from the operator API perspective

Timeline query paths must not:

- trigger remediation
- trigger approval or execution flows
- mutate Kubernetes
- mutate ArgoCD
- mutate GitOps repositories
- update RAG
- update Qdrant

## 9. Metrics and Observability Expectations

Future projection stores may add observability such as:

- projection freshness indicators
- projection lag metrics
- query latency metrics
- degraded projection counters
- backend availability metrics

Low-cardinality metric discipline must remain preserved.

Metric tags must not expose:

- cursor values
- eventId
- recordId
- incidentId
- exception messages
- raw source payload details

## 10. Migration Expectations

Expected future migration behavior:

- runtime aggregation may migrate to a projection store
- frontend and API contract stability must be preserved
- cursor behavior must remain stable
- degraded response semantics must remain stable
- read-only operator behavior must remain stable

Migration may change internal execution and storage strategy, but it must not
change the externally visible Timeline contract.

## 11. Non-goals

This contract does not introduce:

- actual projection table schema
- R2DBC repository implementation
- CDC implementation
- Kafka projection pipeline
- Debezium
- event sourcing migration
- SSE
- WebSocket
