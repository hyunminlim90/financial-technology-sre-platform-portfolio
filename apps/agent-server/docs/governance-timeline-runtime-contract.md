# Governance Timeline Runtime Contract

## Purpose

This document defines operator-facing runtime semantics for governance timeline query and aggregation layers.

Timeline runtime is a lightweight read-only operational contract.

This phase defines DTO and documentation only.

It does not introduce runtime controllers, Micrometer gauges, timeline aggregation execution, DB probes, SSE, WebSocket, or streaming probes.

## Runtime Modes

| Runtime Mode | Meaning |
|---|---|
| `NORMAL` | Timeline query and aggregation is expected to operate normally |
| `DEGRADED_READ_ONLY` | Timeline may return partial degraded read-only data |
| `ATTENTION_REQUIRED` | Timeline requires operator attention due to strict or unavailable behavior |

## Health to Runtime Mapping

Recommended mapping:

| Timeline Health | Runtime Mode |
|---|---|
| `HEALTHY` | `NORMAL` |
| `DEGRADED_CAPABLE` | `DEGRADED_READ_ONLY` |
| `STRICT` | `ATTENTION_REQUIRED` |
| `UNAVAILABLE` | `ATTENTION_REQUIRED` |

## Degraded Signals

Runtime summary may expose degraded signals such as:

- `timeline:DEGRADED_CAPABLE`
- `timeline:FAIL_OPEN_READ_ONLY`
- `timeline:STRICT`
- `timeline:UNAVAILABLE`

These signals are informational only.

## Lightweight Evaluation

Timeline runtime summary must not execute full timeline aggregation queries.

It must not perform:

- source record fan-out
- cursor pagination execution
- projection merge
- DB-heavy timeline queries
- streaming probe

## Operator-facing Semantics

Runtime mode is intended for:

- React SRE Console runtime banner
- timeline panel status badge
- operator awareness
- degraded read-only disclosure

Runtime mode must not be used to trigger automated execution.

## Read-only Guarantees

Timeline runtime summary is observability-only.

It must not:

- trigger remediation
- approve recommendations
- execute plans
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- create Git commits or pull requests
- update RAG
- update Qdrant

## Sensitive Data

Timeline runtime summary must not expose:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts
- cursor values
- record identifiers
- exception messages

## Non-goals

This contract does not introduce:

- runtime controller implementation
- Micrometer gauge implementation
- DB connectivity probe
- timeline aggregation execution
- cursor query execution
- SSE
- WebSocket
- streaming probe
- frontend implementation
- mutation API
