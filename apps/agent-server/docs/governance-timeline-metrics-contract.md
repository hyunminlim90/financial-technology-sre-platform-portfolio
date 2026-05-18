# Governance Timeline Metrics Contract

## Purpose

This document defines Micrometer metric naming and low-cardinality tag semantics for governance timeline query, aggregation, degradation, page size, and health status.

This phase defines contract only.

It does not introduce Micrometer recorder implementation, Prometheus alert rules, Grafana dashboards, or remediation.

## Metrics

| Metric | Type | Purpose |
|---|---|---|
| `fin_sre_governance_timeline_query_total` | Counter | Timeline query result count |
| `fin_sre_governance_timeline_aggregation_total` | Counter | Timeline aggregation result count |
| `fin_sre_governance_timeline_degraded_total` | Counter | Partial degraded timeline response count |
| `fin_sre_governance_timeline_page_size` | DistributionSummary | Timeline page item count |
| `fin_sre_governance_timeline_health_status` | Gauge | Timeline health status |

## Query Metric Semantics

`fin_sre_governance_timeline_query_total`

Allowed tags:

| Tag | Allowed Values |
|---|---|
| `result` | `success`, `empty`, `failure` |
| `mode` | `STRICT`, `PARTIAL_DEGRADED`, `FAIL_OPEN_READ_ONLY` |

The query metric records the externally visible timeline query result.

## Aggregation Metric Semantics

`fin_sre_governance_timeline_aggregation_total`

Allowed tags:

| Tag | Allowed Values |
|---|---|
| `result` | `success`, `degraded`, `failure` |
| `mode` | `STRICT`, `PARTIAL_DEGRADED`, `FAIL_OPEN_READ_ONLY` |

Aggregation metrics observe projection, merge, and page construction outcomes.

## Degraded Metric Semantics

`fin_sre_governance_timeline_degraded_total`

Allowed tags:

| Tag | Allowed Values |
|---|---|
| `reason` | `component_query_failed`, `component_query_timeout`, `projection_failed`, `aggregation_degraded`, `timeline_query_timeout` |
| `source` | values from `GovernanceTimelineAggregationSource` |
| `mode` | `PARTIAL_DEGRADED`, `FAIL_OPEN_READ_ONLY` |

This metric must only increment when a timeline response is actually degraded.

## Page Size Metric Semantics

`fin_sre_governance_timeline_page_size`

This metric records returned timeline item count.

Allowed tags:

| Tag | Allowed Values |
|---|---|
| `mode` | `STRICT`, `PARTIAL_DEGRADED`, `FAIL_OPEN_READ_ONLY` |

The metric must not include cursor, incidentId, recommendationRecordId, or query text.

## Health Gauge Semantics

`fin_sre_governance_timeline_health_status`

Recommended value mapping:

| Status | Value |
|---|---:|
| `HEALTHY` | 0 |
| `DEGRADED_CAPABLE` | 1 |
| `STRICT` | 2 |
| `UNAVAILABLE` | 3 |

## Low-cardinality Rules

Timeline metrics must never use the following as tags:

- cursor
- eventId
- recordId
- incidentId
- recommendationRecordId
- learningCandidateId
- knowledgeUpdateApplicationId
- search query text
- exception message
- summary text
- file path
- git commit SHA
- operatorId
- customer identifier
- payment identifier

## Observability-only Principle

Timeline metrics are observability-only.

They must not:

- trigger remediation
- approve recommendations
- execute plans
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- update RAG
- update Qdrant

## Non-goals

This contract does not introduce:

- Micrometer recorder implementation
- Prometheus alert rules
- Grafana dashboards
- SLO alert policies
- automatic remediation
- timeline query implementation
- streaming implementation
