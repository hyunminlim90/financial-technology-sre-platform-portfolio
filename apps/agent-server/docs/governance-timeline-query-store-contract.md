# Governance Timeline Query Store Contract

## 1. Timeline Query Store Goals

This document defines the storage-facing contract for future Governance Timeline
query backends.

The goal is to preserve the current HTTP API, cursor behavior, degradation
semantics, and read-only audit guarantees while allowing the underlying query
implementation to evolve.

## 2. Current In-memory Aggregation Characteristics

The current Governance Timeline implementation uses in-memory aggregation and
merge semantics.

Current characteristics:

- source record fan-out and merge occur in application memory
- timeline events are projected and deduplicated after source reads
- large-scale timeline query workloads are not yet read-store optimized
- fan-out aggregation cost exists for multi-source timeline queries

The current model is correct for contract validation and read-only operator
usage, but it is not the final scaling model for large timeline history views.

## 3. Future Query Store Targets

Future query store targets may include:

- R2DBC and PostgreSQL-backed timeline projection queries
- materialized read model storage
- read-optimized timeline store implementations
- future search-oriented timeline integration
- OpenSearch or Elasticsearch-backed read/query support

Any future store must preserve the externally visible Timeline API contract.

## 4. Query Consistency Requirements

Future query stores must preserve the current operator-visible consistency
semantics:

- append-only audit behavior
- best-effort read consistency under concurrent inserts
- stable page construction for repeated cursor navigation
- deterministic ordering and deduplication rules

Perfect snapshot isolation is not required if stable pagination and duplicate
avoidance are preserved.

## 5. Cursor Ordering Guarantees

The following guarantees must remain stable across any future query store:

- ordering remains `occurredAt DESC, eventId DESC`
- cursor semantics remain opaque
- `NEXT` continues to return older events
- `PREVIOUS` continues to return newer events
- stable pagination behavior remains preserved across pages

Cursor transport format may evolve internally, but frontend semantics must not
change.

## 6. Deduplication Guarantees

Future query stores must preserve:

- `eventId`-based deduplication
- source merge semantics across governance event sources
- deterministic event selection when duplicate projections are detected

The query layer must not reintroduce duplicate timeline rows for the same
logical event.

## 7. Degraded Query Semantics

Future query stores must preserve degraded timeline behavior:

- partial degraded query remains supported
- best-effort read availability remains supported
- failed source isolation remains visible
- successful timeline events remain renderable when possible
- exception messages and raw backend details remain hidden

Changing the storage backend must not remove degraded read-only semantics.

## 8. Read-only and Append-only Guarantees

The timeline query store remains:

- read-only
- append-only in audit semantics
- non-mutating from the operator API perspective

Timeline query execution must not:

- trigger remediation
- trigger approval or execution workflows
- mutate Kubernetes
- mutate ArgoCD
- mutate GitOps repositories
- update RAG
- update Qdrant

## 9. Metrics and Observability Expectations

Future query stores may add backend-specific observability, including:

- query latency metrics
- store-level performance metrics
- degraded query counters
- backend availability metrics

All metrics must preserve low-cardinality tag discipline.

Metric tags must not include:

- cursor values
- eventId
- recordId
- incidentId
- raw query text
- exception messages

## 10. Migration Expectations

Expected future migration path:

- in-memory aggregation may migrate to a persistent query store
- API contract stability must be preserved during migration
- frontend cursor contract must remain stable
- degraded response behavior must remain stable
- runtime and health interpretation must remain stable

Migration is allowed to change internal execution strategy, but not the
operator-facing query semantics.

## 11. Non-goals

This contract does not introduce:

- actual R2DBC implementation
- actual PostgreSQL schema
- projection table implementation
- OpenSearch or Elasticsearch integration
- Kafka streaming
- SSE
- WebSocket
- repository implementation
- performance benchmark implementation
