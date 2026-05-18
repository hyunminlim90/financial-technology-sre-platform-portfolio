# Governance Detail Query Contract

## Purpose

This document defines read-only detail query contracts for the React SRE Console governance views.

The contract describes the detail payloads that the console may expect when navigating from dashboard overview, summary, backlog, trends, risk indicators, and health views.

This contract does not introduce React implementation, mutation APIs, approval buttons, execution buttons, remediation actions, GitOps changes, RAG ingestion, or Qdrant updates.

Sensitive fields must not be included in detail payloads.

Forbidden examples:

- secret
- token
- payload
- rawLog
- payment payload

## Incident Detail Contract

Incident detail is a read-only aggregate view for incident-level governance history.

Recommended response shape:

- incident lifecycle
- recommendations
- approvals
- execution plans
- verification results
- postmortem links
- learning links
- timeline

Recommended response DTO:

- `GovernanceIncidentDetailResponse`

## Recommendation Detail Contract

Recommendation detail is a read-only aggregate view for one recommendation record.

Recommended response shape:

- recommendation summary
- approval history
- execution plan list
- human execution result list
- verification result list
- timeline

Recommended response DTO:

- `GovernanceRecommendationDetailResponse`

## Learning Detail Contract

Learning detail is a read-only aggregate view for one learning candidate.

Recommended response shape:

- learning candidate
- promotion review history
- promotion plan list
- knowledge update application list
- timeline

Recommended response DTO:

- `GovernanceLearningDetailResponse`

## Knowledge Update Detail Contract

Knowledge update detail is a read-only audit view for one applied change record.

Recommended response shape:

- knowledge type
- knowledge layer
- file path
- change type
- git repository
- git branch
- git commit SHA
- pull request reference
- validation checks
- timeline

Recommended response DTO:

- `GovernanceKnowledgeUpdateDetailResponse`

## Timeline Contract

Timeline items represent ordered read-only governance events.

Recommended timeline fields:

- occurredAt
- type
- referenceId
- status
- title
- summary

Recommended timeline DTO:

- `GovernanceDetailTimelineItem`

## Time-range Preservation

Detail navigation should preserve dashboard query context whenever possible.

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
knowledgeUpdateApplicationId
```

If both `from` and `to` are provided, both must be preserved together.

## Internal-only Security

All governance detail views are internal-only.

Protected path expectation:

```text
/internal/**
```

The React SRE Console must not expose governance detail views as public product APIs.

## Read-only Principle

Detail APIs are read-only operational query contracts.

They must not:

- execute remediation
- approve recommendations
- execute plans
- close incidents automatically
- modify GitOps state
- create Git commit or PR
- trigger RAG ingestion
- update Qdrant

## Non-goals

This contract does not define:

- React route implementation
- React component implementation
- mutation workflow
- approve buttons
- execute buttons
- rollback buttons
- Kubernetes mutation
- ArgoCD mutation
- automated learning promotion
