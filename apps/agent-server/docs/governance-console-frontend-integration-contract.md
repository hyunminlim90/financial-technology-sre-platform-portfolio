# Governance Console Frontend Integration Contract

## Purpose

This document defines how the React SRE Console should consume governance backend APIs.

The console is an internal, read-only operational UI.

This contract does not introduce remediation actions, approval execution, GitOps mutation, RAG ingestion, or Qdrant updates.

## Runtime Banner Fetch

The console should fetch runtime summary first.

```text
GET /internal/governance/console/runtime-summary
```

Recommended usage:

- render the runtime banner before dashboard panels
- keep the banner visible across dashboard, search, and detail pages
- display `runtimeMode` and `degradedSignals`
- render `timelineRuntime` and timeline degraded signals when present
- do not expose action buttons from the banner

The timeline runtime summary inside `/internal/governance/console/runtime-summary` is lightweight and read-only.

## Dashboard Initial Load Sequence

Recommended sequence:

1. Fetch `/internal/governance/console/runtime-summary`
2. Fetch `/internal/governance/dashboard/overview`
3. Render dashboard cards, backlog, trends, risk indicators
4. Fetch full detail APIs only when the user navigates

The dashboard should prefer overview APIs before full detail APIs.

## Detail Page Fetch Strategy

Recommended detail page sequence:

1. Fetch lightweight overview preview
2. Render skeleton or detail header
3. Fetch full aggregate detail API
4. Merge full detail data into the page

Examples:

```text
GET /internal/governance/details/overview/incidents/{incidentId}
GET /internal/governance/details/incidents/{incidentId}
```

This improves perceived latency and keeps the UI usable when full detail aggregation is slower.

## Search UX Rules

Search entry point:

```text
GET /internal/governance/search?q={keyword}&type=ALL&window=24h&limit=20
```

Recommended behavior:

- use search for navigation and discovery
- render search results as lightweight rows
- clicking a result should first open overview preview
- full detail should load only after explicit navigation or panel expansion
- partial degraded search results must be visibly marked

## Timeline UX Rules

Timeline entry point:

```text
GET /internal/governance/timeline
```

Recommended behavior:

- use the timeline panel for read-only audit navigation
- render timeline rows as immutable governance events
- treat timeline runtime signals as informational only
- use `/internal/governance/timeline/runtime-summary` for timeline runtime badges or banners
- use `/internal/governance/timeline/health` for lightweight timeline health inspection

## Degraded Response Rendering

If a response contains degradation metadata with `degraded=true`, the UI must:

- display a degraded read-only badge
- show `failedComponents` when provided
- show the degradation reason
- clearly mark that some data may be partial
- avoid hiding successful components

Degraded data is still safe to render, but must not be presented as complete.

Timeline degraded signals must also be displayed as informational only.

## UI States

### Loading

Use skeleton cards or placeholders.

### Empty

Show neutral empty states, such as:

```text
No governance records found for the selected time range.
```

### Not Found

For 404 detail responses:

```text
The requested governance record was not found.
```

### Forbidden

For 403 responses:

```text
You do not have permission to access this internal governance console.
```

### Disabled or Hidden

For 404 internal route disabled cases:

```text
This internal governance view is not available.
```

## Polling Guidance

Recommended intervals:

| Area | Suggested Interval |
|---|---:|
| Runtime summary | 30s |
| Timeline runtime summary | 30s to 60s |
| Timeline health | 60s |
| Dashboard overview | 30s to 60s |
| Health APIs | 60s |
| Search | user-triggered only |
| Timeline | on demand |
| Full detail APIs | on demand |
| Detail overview preview | on demand |

The frontend should avoid aggressive polling of full detail APIs.

## Read-only UX Guarantees

The frontend must treat governance APIs as read-only.

The UI must not expose buttons or workflows that:

- approve recommendations
- execute plans
- trigger remediation
- restart services
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- create Git commits or pull requests
- update RAG
- update Qdrant

## Sensitive Data Policy

The frontend must not display or log:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full LLM prompts

The frontend must not use record IDs, search query text, or exception messages as metric tags.

## Internal-only Deployment Assumptions

The React SRE Console must be deployed as an internal operational UI.

It must not expose governance APIs as public product APIs.

Recommended assumptions:

- protected internal route
- internal auth boundary
- no public unauthenticated access
- no browser-side secret hardcoding

## Related APIs

Runtime:

- `/internal/governance/console/runtime-summary`
- `/internal/governance/console/health`

Dashboard:

- `/internal/governance/dashboard/overview`
- `/internal/governance/dashboard/summary`
- `/internal/governance/dashboard/backlog`
- `/internal/governance/dashboard/trends`
- `/internal/governance/dashboard/risk-indicators`
- `/internal/governance/dashboard/health`

Search:

- `/internal/governance/search`
- `/internal/governance/search/health`

Timeline:

- `/internal/governance/timeline`
- `/internal/governance/timeline/health`
- `/internal/governance/timeline/runtime-summary`

Detail:

- `/internal/governance/details/overview/**`
- `/internal/governance/details/incidents/{incidentId}`
- `/internal/governance/details/recommendations/{recommendationRecordId}`
- `/internal/governance/details/learning-candidates/{learningCandidateId}`
- `/internal/governance/details/knowledge-updates/{knowledgeUpdateApplicationId}`
- `/internal/governance/details/health`
