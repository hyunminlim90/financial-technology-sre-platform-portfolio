# Governance Timeline Operator Query Guide

## Governance Timeline Overview

The governance timeline is a read-only, append-only operational audit view for governance lifecycle events.

It provides a merged timeline across recommendation, approval, execution planning, human execution, verification, incident lifecycle, postmortem, learning, and knowledge update activity.

Timeline APIs are internal-only and intended for operator, SRE, and internal frontend use.

## Timeline API Endpoints

### Query Timeline

```http
GET /internal/governance/timeline?limit=50
```

### Query Older Events

```http
GET /internal/governance/timeline?cursor=<opaque>&direction=NEXT
```

### Query Newer Events

```http
GET /internal/governance/timeline?cursor=<opaque>&direction=PREVIOUS
```

### Filter by Event Type

```http
GET /internal/governance/timeline?eventType=INCIDENT_TRANSITIONED
```

### Runtime Summary

```http
GET /internal/governance/timeline/runtime-summary
```

### Health

```http
GET /internal/governance/timeline/health
```

## Query Parameter Semantics

Supported timeline query parameters:

- `cursor`
- `direction`
- `limit`
- `from`
- `to`
- `eventType`
- `includeDegraded`

Interpretation:

- `cursor`: opaque pagination token returned by the previous timeline response
- `direction`: `NEXT` or `PREVIOUS`
- `limit`: requested page size, clamped server-side
- `from` and `to`: optional bounded time range and must be provided together
- `eventType`: one or more timeline event type filters
- `includeDegraded`: operator hint for degraded timeline disclosure semantics

## Cursor Pagination Behavior

Timeline cursors are opaque Base64 URL-safe tokens.

Operators and frontend clients must not:

- parse cursor contents
- infer record IDs from cursors
- modify cursor values
- log cursor values as user-visible diagnostics
- use cursor values as metric tags

The cursor is transport-only.

## NEXT / PREVIOUS Semantics

Timeline ordering is:

```text
occurredAt DESC, eventId DESC
```

With that ordering:

- `NEXT` returns older events
- `PREVIOUS` returns newer events

Examples:

- no cursor: current newest page
- `cursor + NEXT`: older page after the current page
- `cursor + PREVIOUS`: newer page before the current page

## Stable Ordering Guarantees

Timeline responses use deterministic ordering:

```text
occurredAt DESC, eventId DESC
```

This means:

- newer events sort first
- equal timestamps use `eventId` as a deterministic tie-breaker
- client-side rendering should preserve server order

## Degraded Timeline Semantics

Timeline may return partial degraded read-only results.

Degraded timeline responses:

- never execute remediation
- never mutate Kubernetes or GitOps
- never trigger approval or execution
- never update RAG or Qdrant
- may still contain successful events
- must disclose degraded or partial status to the operator

When degraded:

- response status may be `DEGRADED`
- degradation metadata may identify failed components
- degraded reasons remain low-cardinality and operator-safe

## Runtime Summary Semantics

Timeline runtime modes:

- `NORMAL`
- `DEGRADED_READ_ONLY`
- `ATTENTION_REQUIRED`

Interpretation:

- `NORMAL`: timeline query and aggregation is operating normally
- `DEGRADED_READ_ONLY`: timeline remains available with partial degraded read-only semantics
- `ATTENTION_REQUIRED`: timeline requires operator attention because strict or unavailable behavior is active

Timeline runtime summary is lightweight.

It does not execute aggregation, DB fan-out, cursor traversal, or streaming probes.

## Health State Semantics

Timeline health states:

- `HEALTHY`
- `DEGRADED_CAPABLE`
- `STRICT`
- `UNAVAILABLE`

Interpretation:

- `HEALTHY`: normal strict read-only behavior is available
- `DEGRADED_CAPABLE`: partial degraded timeline response is supported
- `STRICT`: component failure may fail the timeline query
- `UNAVAILABLE`: required timeline aggregation dependency is unavailable

The health API is lightweight and configuration-aware.

It does not execute timeline aggregation, cursor queries, or streaming probes.

## Metrics Interpretation

Timeline metrics include:

- `fin_sre_governance_timeline_query_total`
- `fin_sre_governance_timeline_degraded_total`
- `fin_sre_governance_timeline_page_size`
- `fin_sre_governance_timeline_health_status`
- `fin_sre_governance_timeline_runtime_mode`

Operator interpretation:

- `fin_sre_governance_timeline_query_total`: visible query outcome volume
- `fin_sre_governance_timeline_degraded_total`: partial degraded timeline events by failed source and reason
- `fin_sre_governance_timeline_page_size`: returned timeline page size distribution
- `fin_sre_governance_timeline_health_status`: current timeline health gauge
- `fin_sre_governance_timeline_runtime_mode`: operator-facing runtime mode gauge

Metrics must remain low-cardinality.

Do not use:

- cursor
- eventId
- raw query text
- raw exception messages

as metric or log tags.

## Read-only Guarantees

Timeline APIs are internal-only, append-only, and read-only.

They must not:

- trigger remediation side-effects
- trigger execution side-effects
- approve recommendations
- execute plans
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- update RAG
- update Qdrant

## Sensitive Data Policy

Timeline APIs and related metrics must not expose:

- secrets
- tokens
- customer data
- payment payloads
- raw logs
- full prompts
- cursor contents
- raw exception details

Sanitized operator-safe summaries only should be rendered.

## Non-goals

This guide does not introduce:

- SSE or streaming documentation
- realtime timeline implementation guidance
- Grafana or PrometheusRule documentation
- R2DBC optimized timeline query documentation
