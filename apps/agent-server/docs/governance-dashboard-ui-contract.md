# Governance Dashboard UI Contract

## Purpose

This document defines the UI contract between the React SRE Console and the agent-server governance dashboard APIs.

The dashboard is an internal operational console.

It is read-only and must not trigger remediation, GitOps changes, RAG ingestion, or Qdrant updates.

## APIs

### Overview

```http
GET /internal/governance/dashboard/overview?window=24h&bucket=1h
```

Used for the first screen.

Includes:

- summary
- backlog
- trends
- riskIndicators

### Summary

```http
GET /internal/governance/dashboard/summary?window=24h
```

Used for top-level count cards.

### Backlog

```http
GET /internal/governance/dashboard/backlog?window=24h
```

Used for operational work queue cards.

### Trends

```http
GET /internal/governance/dashboard/trends?window=24h&bucket=1h
```

Used for trend charts.

### Risk Indicators

```http
GET /internal/governance/dashboard/risk-indicators?window=24h
```

Used for risk cards and alert badges.

## Dashboard Layout

The first dashboard screen should contain:

1. Risk Overview
2. Governance Summary Cards
3. Backlog Queue
4. Trend Charts
5. Learning Governance Section

## Risk Level Rendering

| Risk Level | UI Meaning |
|---|---|
| LOW | Normal |
| MEDIUM | Attention required |
| HIGH | Operational risk |
| CRITICAL | Immediate review required |

Risk indicators must be rendered as read-only signals.

They must not provide action buttons that trigger remediation.

## Summary Cards

Recommended cards:

- Recommendations
- Approvals
- Execution Plans
- Human Executions
- Verifications
- Incidents
- Postmortem Drafts
- Learning Candidates
- Knowledge Updates

Each card should display:

- title
- primary count
- status breakdown
- time window

## Backlog Queue

Backlog items represent operational work queues.

Recommended items:

- pendingRecommendationApprovals
- approvedRecommendationsWithoutExecutionPlan
- executionResultsAwaitingVerification
- unresolvedIncidents
- postmortemDraftsAwaitingReview
- learningCandidatesAwaitingPromotionReview
- promotionPlansAwaitingApplication

Backlog items are read-only navigation hints.

They must not trigger automatic approval, execution, or knowledge promotion.

## Trend Charts

Trend charts use:

```json
{
  "name": "approvalDecisions",
  "points": [
    {
      "bucketStart": "2026-05-09T00:00:00Z",
      "bucketEnd": "2026-05-09T01:00:00Z",
      "total": 3,
      "byStatus": {
        "APPROVED": 2,
        "REJECTED": 1
      }
    }
  ]
}
```

Recommended charts:

- recommendationsCreated
- approvalDecisions
- verificationResults
- incidentLifecycleTransitions
- learningCandidates
- knowledgeUpdates

## Time Window Selector

Supported presets:

- 1h
- 24h
- 7d

Supported buckets:

- 15m
- 1h
- 1d

Recommended mapping:

| Window | Default Bucket |
|---|---|
| 1h | 15m |
| 24h | 1h |
| 7d | 1d |

## UI States

### Loading

The dashboard should show skeleton cards.

### Empty

When all counts are zero:

```text
No governance activity in the selected time range.
```

### Error

For 400 query validation errors:

```text
Invalid dashboard time range.
```

For 403:

```text
You do not have permission to view internal governance dashboard.
```

For 404:

```text
Internal governance dashboard is disabled or not exposed.
```

## Security Contract

All dashboard APIs are internal-only.

Protected path:

```text
/internal/governance/**
```

The React SRE Console must call these APIs only through an authenticated internal console backend or protected internal route.

The dashboard must not be exposed as public product API.

## Non-goals

The dashboard UI must not:

- execute remediation
- approve recommendations automatically
- mutate GitOps state
- call Kubernetes directly
- trigger RAG ingestion
- update Qdrant
- expose sensitive metadata
