# Governance Console API Contract Summary

## API Groups

### Dashboard APIs

- `GET /internal/governance/dashboard/overview`
- `GET /internal/governance/dashboard/summary`
- `GET /internal/governance/dashboard/backlog`
- `GET /internal/governance/dashboard/trends`
- `GET /internal/governance/dashboard/risk-indicators`
- `GET /internal/governance/dashboard/health`

### Full Detail APIs

- `GET /internal/governance/details/incidents/{incidentId}`
- `GET /internal/governance/details/recommendations/{recommendationRecordId}`
- `GET /internal/governance/details/learning-candidates/{learningCandidateId}`
- `GET /internal/governance/details/knowledge-updates/{knowledgeUpdateApplicationId}`
- `GET /internal/governance/details/health`

### Lightweight Detail Overview APIs

- `GET /internal/governance/details/overview/incidents/{incidentId}`
- `GET /internal/governance/details/overview/recommendations/{recommendationRecordId}`
- `GET /internal/governance/details/overview/learning-candidates/{learningCandidateId}`
- `GET /internal/governance/details/overview/knowledge-updates/{knowledgeUpdateApplicationId}`

### Timeline APIs

- `GET /internal/governance/timeline`
- `GET /internal/governance/timeline/health`
- `GET /internal/governance/timeline/runtime-summary`

## UI Usage

| UI Area | Recommended API |
|---|---|
| Console landing page | `/internal/governance/dashboard/overview` |
| Top-level count cards | `/internal/governance/dashboard/summary` |
| Work queue cards | `/internal/governance/dashboard/backlog` |
| Trend charts | `/internal/governance/dashboard/trends` |
| Risk badges | `/internal/governance/dashboard/risk-indicators` |
| Dashboard health badge | `/internal/governance/dashboard/health` |
| Console runtime banner | `/internal/governance/console/runtime-summary` includes `timelineRuntime` |
| Timeline panel | `/internal/governance/timeline` |
| Timeline health badge | `/internal/governance/timeline/health` |
| Timeline runtime badge or banner | `/internal/governance/timeline/runtime-summary` |
| Detail side panel preview | `/internal/governance/details/overview/**` |
| Full incident page | `/internal/governance/details/incidents/{incidentId}` |
| Full recommendation page | `/internal/governance/details/recommendations/{recommendationRecordId}` |
| Full learning page | `/internal/governance/details/learning-candidates/{learningCandidateId}` |
| Full knowledge update page | `/internal/governance/details/knowledge-updates/{knowledgeUpdateApplicationId}` |
| Detail health badge | `/internal/governance/details/health` |

## Query Parameters

Dashboard APIs support:

| Parameter | Meaning |
|---|---|
| `window` | `1h`, `24h`, `7d` |
| `from` / `to` | Custom time range. Must be provided together. |
| `bucket` | `15m`, `1h`, `1d` for trend APIs |

Rules:

- `from` and `to` must be provided together.
- `from` must be before or equal to `to`.
- unsupported `window` or `bucket` returns 400.

## Degradation Semantics

Dashboard APIs may include dashboard degradation metadata.

Detail APIs may include detail degradation metadata.

Timeline APIs may include timeline degradation metadata and runtime degraded signals.

Degraded response means:

- API returned read-only partial or fallback data
- some optimized query or child component query failed
- response is still safe to render
- no remediation or mutation was triggered

## Health States

### Dashboard Health

| Status | Meaning |
|---|---|
| `HEALTHY` | Optimized query layer is available |
| `DEGRADED` | Fallback aggregation is being used or available |
| `UNAVAILABLE` | Dashboard cannot provide safe fallback |

### Detail Health

| Status | Meaning |
|---|---|
| `HEALTHY` | Detail APIs use default strict behavior |
| `DEGRADED_CAPABLE` | Partial degraded detail response is available |
| `STRICT` | Child component failures fail the request |

### Timeline Health

| Status | Meaning |
|---|---|
| `HEALTHY` | Timeline query and aggregation contract is available with normal behavior |
| `DEGRADED_CAPABLE` | Partial degraded timeline response is available |
| `STRICT` | Component failure may fail timeline query responses |
| `UNAVAILABLE` | Timeline query or aggregation layer is unavailable |

## Runtime Summary

`GET /internal/governance/console/runtime-summary` includes:

- `consoleHealth`
- `dashboardHealth`
- `detailHealth`
- `searchHealth`
- `timelineRuntime`
- `degradedSignals`

Timeline runtime integration is read-only and does not execute timeline aggregation, cursor queries, DB fan-out, or streaming probes.

## Metrics

Dashboard metrics include:

- `fin_sre_governance_dashboard_degraded_total`
- `fin_sre_governance_dashboard_health_status`
- `fin_sre_governance_query_optimized_total`
- `fin_sre_governance_query_fallback_total`
- `fin_sre_governance_query_failure_total`

Detail metrics include:

- `fin_sre_governance_detail_query_total`
- `fin_sre_governance_detail_query_not_found_total`
- `fin_sre_governance_detail_degraded_total`
- `fin_sre_governance_detail_health_status`
- `fin_sre_governance_detail_overview_query_total`
- `fin_sre_governance_detail_overview_degraded_total`

Timeline metrics include:

- `fin_sre_governance_timeline_query_total`
- `fin_sre_governance_timeline_degraded_total`
- `fin_sre_governance_timeline_page_size`
- `fin_sre_governance_timeline_health_status`
- `fin_sre_governance_timeline_runtime_mode`

Metrics must use low-cardinality tags only.

## Security Contract

All governance console APIs are internal-only.

Protected paths:

```text
/internal/governance/**
/internal/governance/details/**
```

These APIs must not be exposed as public product APIs.

## Read-only Guarantees

Governance console APIs must not:

- approve recommendations
- execute plans
- trigger remediation
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- create Git commits or pull requests
- update RAG
- update Qdrant
- store or expose sensitive payloads

## Sensitive Data Policy

Responses and metrics must not expose:

- payment payload
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts
- high-cardinality identifiers as metric tags

## Related Documents

- `docs/governance-dashboard-ui-contract.md`
- `docs/governance-dashboard-navigation-contract.md`
- `docs/governance-detail-query-contract.md`
- `docs/governance-query-resilience-policy.md`
- `docs/governance-detail-query-resilience-policy.md`
- `docs/r2dbc-governance-schema.md`
- `docs/governance-retention-archival-policy.md`
