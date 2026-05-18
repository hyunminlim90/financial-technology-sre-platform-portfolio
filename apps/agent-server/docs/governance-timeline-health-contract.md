# Governance Timeline Health Contract

## Purpose

This document defines resilience-aware health semantics for governance timeline query and aggregation layers.

Timeline health is a lightweight operational contract.

This phase defines DTO and documentation only.

It does not introduce health controllers, DB probes, timeline aggregation execution, Micrometer gauges, SSE, WebSocket, or streaming probes.

## Health States

| Status | Meaning |
|---|---|
| `HEALTHY` | Timeline query and aggregation contract is available with normal behavior |
| `DEGRADED_CAPABLE` | Timeline can return partial degraded read-only responses |
| `STRICT` | Component failure may fail timeline query responses |
| `UNAVAILABLE` | Timeline query and aggregation layer is unavailable |

## Response Contract

Recommended response fields:

- `checkedAt`
- `status`
- `resilienceMode`
- `partialTimelineSupported`
- `failOpenReadOnly`
- `streamingCompatible`
- `degradedReasonTaxonomy`
- `message`

## Degraded Reason Taxonomy

Allowed low-cardinality reasons:

- `component_query_failed`
- `component_query_timeout`
- `projection_failed`
- `aggregation_degraded`
- `timeline_query_timeout`

Exception messages, stack traces, SQL errors, and DB internals must not be exposed.

## Lightweight Evaluation

Timeline health evaluation must not execute full timeline aggregation queries.

Timeline health must not perform:

- large DB fan-out
- cursor pagination execution
- streaming query execution
- source record scan
- projection aggregation

## Read-only Guarantees

Timeline health is observability-only.

It must not:

- trigger remediation
- approve recommendations
- execute plans
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- update RAG
- update Qdrant

## Streaming Compatibility

Timeline health should remain compatible with future:

- cursor pagination
- incremental polling
- WebFlux streaming
- SSE-based timeline updates

Streaming compatibility does not mean the health check should open a stream.

## Sensitive Data

Timeline health response must not expose:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts
- cursor values
- record identifiers

## Non-goals

This contract does not introduce:

- actual health API controller
- DB connectivity probe
- timeline aggregation execution
- Micrometer gauge
- Prometheus alert rules
- Grafana dashboard
- SSE
- WebSocket
- streaming probe
- mutation API
