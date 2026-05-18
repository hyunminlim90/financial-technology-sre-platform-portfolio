# Governance Timeline Mapping Contract

## Purpose

This document defines how governance domain records should be projected into normalized `GovernanceTimelineEvent` read-model events.

Timeline projection is read-only and append-only.

This phase defines contract only.

It does not introduce actual mappers, DB projection tables, Kafka events, SSE, WebSocket, or streaming.

## Source to Event Mapping

| Source Record | Timeline Event Type |
|---|---|
| `RecommendationRecord` | `RECOMMENDATION_CREATED` |
| `RecommendationApprovalRecord` | `APPROVAL_DECIDED` |
| `RecommendationExecutionPlan` | `EXECUTION_PLAN_CREATED` |
| `HumanExecutionResultRecord` | `HUMAN_EXECUTION_RECORDED` |
| `VerificationResultRecord` | `VERIFICATION_RECORDED` |
| `IncidentLifecycleRecord` | `INCIDENT_TRANSITIONED` |
| `PostmortemDraftRecord` | `POSTMORTEM_DRAFT_CREATED` |
| `PostmortemReviewRecord` | `POSTMORTEM_REVIEWED` |
| `LearningCandidateRecord` | `LEARNING_CANDIDATE_CREATED` |
| `KnowledgePromotionReviewRecord` | `PROMOTION_REVIEWED` |
| `KnowledgePromotionPlanRecord` | `PROMOTION_PLAN_CREATED` |
| `KnowledgeUpdateApplicationRecord` | `KNOWLEDGE_UPDATED` |

## Actor Mapping

Recommended actor mapping:

| Source | Actor Type |
|---|---|
| AI recommendation or postmortem draft | `AI` |
| approval, verification, review, or knowledge update application | `HUMAN` |
| lifecycle or system-derived projection | `SYSTEM` |

## Resource Mapping

Recommended resource mapping:

| Source | Resource Type |
|---|---|
| Incident lifecycle | `INCIDENT` |
| Recommendation record | `RECOMMENDATION` |
| Approval record | `APPROVAL` |
| Execution plan | `EXECUTION_PLAN` |
| Human execution result | `HUMAN_EXECUTION` |
| Verification result | `VERIFICATION` |
| Postmortem draft or review | `POSTMORTEM` |
| Learning candidate | `LEARNING` |
| Knowledge promotion review or plan | `KNOWLEDGE_PROMOTION` |
| Knowledge update application | `KNOWLEDGE_UPDATE` |

## Severity Mapping

Recommended severity mapping:

| Condition | Severity |
|---|---|
| Normal creation or record append | `INFO` |
| Pending review, degraded projection, or partial source | `WARNING` |
| Rejected, failed verification, or blocked step | `ERROR` |
| Security or payment integrity critical signal | `CRITICAL` |

## Deterministic Projection

The same source record should produce the same timeline event projection.

Projection should be deterministic based on:

- source type
- source id
- source timestamp
- source status
- sanitized summary

## Projection Event ID

Recommended event ID format:

```text
{sourceType}:{sourceId}
```

The exposed cursor may encode this value, but cursors must remain opaque to the frontend.

## Sanitization

Projection must sanitize all summaries and metadata before timeline exposure.

Projection output must not expose:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts

## Degraded Projection

Projection may be degraded if one or more source components fail.

When degraded:

- successful projections may still be returned
- failed source types must be listed
- reason must be low-cardinality
- exception messages must not be exposed

## Stability and Compatibility

The projected `GovernanceTimelineEvent` shape must remain stable for:

- cursor pagination
- search projection
- React rendering
- incremental polling
- future streaming or SSE

## Non-goals

This contract does not introduce:

- actual mapper implementation
- projection tables
- Kafka events
- WebFlux streaming
- SSE
- WebSocket
- mutation APIs
- remediation
- GitOps mutation
- RAG ingestion
- Qdrant updates
