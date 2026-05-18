# Governance Dashboard Navigation Contract

## Purpose

This document defines read-only navigation behavior for the React SRE Console governance dashboard.

The navigation contract connects overview, summary, backlog, trend, risk, health, incident, recommendation, and learning governance views.

This contract does not introduce mutation actions, remediation buttons, approval execution, GitOps changes, RAG ingestion, or Qdrant updates.

## Dashboard Entry Points

| View | API | Purpose |
|---|---|---|
| Overview | `/internal/governance/dashboard/overview` | First dashboard screen |
| Summary | `/internal/governance/dashboard/summary` | Count and status breakdown detail |
| Backlog | `/internal/governance/dashboard/backlog` | Operational work queue detail |
| Trends | `/internal/governance/dashboard/trends` | Time-bucketed trend detail |
| Risk Indicators | `/internal/governance/dashboard/risk-indicators` | Governance risk detail |
| Health | `/internal/governance/dashboard/health` | Dashboard availability badge/detail |

## Navigation Rules

### Overview to Summary

Summary cards may navigate to read-only detail views.

Examples:

| Card | Target View |
|---|---|
| Recommendations | Recommendation List |
| Approvals | Recommendation Approval List |
| Execution Plans | Execution Plan List |
| Verifications | Verification Result List |
| Incidents | Incident Lifecycle List |
| Postmortems | Postmortem Draft/Review List |
| Learning Candidates | Learning Governance List |
| Knowledge Updates | Knowledge Update Application List |

## Backlog Navigation

Backlog items represent work queues.

| Backlog Item | Target View |
|---|---|
| pendingRecommendationApprovals | Recommendation Approval Queue |
| approvedRecommendationsWithoutExecutionPlan | Execution Plan Candidate Queue |
| executionResultsAwaitingVerification | Verification Queue |
| unresolvedIncidents | Incident Operations Queue |
| postmortemDraftsAwaitingReview | Postmortem Review Queue |
| learningCandidatesAwaitingPromotionReview | Learning Candidate Review Queue |
| promotionPlansAwaitingApplication | Knowledge Update Application Queue |

Backlog navigation is read-only.

The UI may show links to records, but must not perform approve, execute, remediate, Git, or RAG actions directly from dashboard cards.

## Trend Navigation

Trend chart points may navigate to filtered detail lists.

Required context to preserve:

- window
- from
- to
- bucketStart
- bucketEnd
- series name
- status key

Example:

```text
approvalDecisions bucket=2026-05-09T10:00:00Z~2026-05-09T11:00:00Z status=REJECTED
→ Recommendation Approval List filtered by time range and status
```

## Risk Navigation

Risk indicator cards may navigate to investigation views.

| Risk Indicator | Target View |
|---|---|
| approvalRejectRate | Rejected Approval Detail |
| verificationFailureRate | Failed Verification Detail |
| incidentReopenRate | Reopened/Escalated Incident Detail |
| learningBacklog | Learning Candidate Review Queue |
| promotionPlanBacklog | Knowledge Promotion Plan Queue |
| postmortemRevisionRate | Postmortem Needs Revision Detail |

Risk indicators are advisory signals.

They must not trigger remediation, escalation, rollback, approval, or Git/RAG actions.

## Health Navigation

The dashboard health badge may navigate to a read-only health detail panel.

Health detail should display:

- status
- optimizedQueryAvailable
- fallbackEnabled
- failOpenDashboard
- resilienceEnabled
- lastDegradationReason
- message

Health detail must not provide buttons to restart services, change configuration, or modify GitOps state.

## Detail View Categories

Recommended read-only detail views:

| Category | Description |
|---|---|
| Incident Detail | Lifecycle, recommendations, verification, postmortem, learning links |
| Recommendation Detail | Recommendation, approval, execution plan, human result, verification |
| Postmortem Detail | Draft, review history, learning candidate |
| Learning Detail | Candidate, promotion review, promotion plan |
| Knowledge Update Detail | Applied file path, commit SHA, PR reference, validation checks |
| Dashboard Health Detail | Query optimization, fallback, degradation state |

## Query Context Contract

Navigation must preserve time context.

Recommended parameters:

```text
window
from
to
bucket
bucketStart
bucketEnd
status
series
incidentId
recommendationRecordId
learningCandidateId
promotionPlanId
```

If both from and to are provided, both must be preserved together.

## Security Contract

All navigation targets are internal-only.

Protected path:

```text
/internal/**
```

The React SRE Console must not expose governance detail views as public product APIs.

All navigation is read-only unless a separate human approval workflow explicitly exists.

## Non-goals

This contract does not define:

- React component implementation
- route implementation
- mutation APIs
- approve buttons
- execute buttons
- remediation actions
- Git commit or PR creation
- RAG ingestion
- Qdrant update
- Kubernetes or ArgoCD mutation
