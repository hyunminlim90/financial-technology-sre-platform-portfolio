# Governance Timeline Query Contract

## Purpose

This document defines query, filter, and event taxonomy contract for governance timeline APIs.

Governance timelines are read-only append-only operational audit views.

This phase defines contract only.

It does not introduce controllers, DB cursor queries, SSE, WebSocket, streaming, or frontend implementation.

## Query Parameters

Recommended future query parameters:

| Parameter | Meaning |
|---|---|
| `cursor` | Opaque cursor for pagination |
| `limit` | Page size |
| `direction` | `NEXT` or `PREVIOUS` |
| `from` / `to` | Optional time range |
| `eventType` | Single or repeated event type |
| `includeDegraded` | Whether degraded timeline metadata should be included |

Rules:

- `limit` must be clamped server-side.
- cursor must be opaque.
- `from` and `to` must be provided together when used as a bounded range.
- filters must remain stable across cursor pagination requests.
- changing filters invalidates existing cursors.

## Event Taxonomy

Supported event types:

- `RECOMMENDATION_CREATED`
- `APPROVAL_DECIDED`
- `EXECUTION_PLAN_CREATED`
- `HUMAN_EXECUTION_RECORDED`
- `VERIFICATION_RECORDED`
- `INCIDENT_TRANSITIONED`
- `POSTMORTEM_DRAFT_CREATED`
- `POSTMORTEM_REVIEWED`
- `LEARNING_CANDIDATE_CREATED`
- `PROMOTION_REVIEWED`
- `PROMOTION_PLAN_CREATED`
- `KNOWLEDGE_UPDATED`

## Filtering Semantics

Timeline queries may support the following scopes:

| Scope | Meaning |
|---|---|
| Incident timeline | Events linked to an incident |
| Recommendation timeline | Events linked to a recommendation |
| Learning timeline | Events linked to a learning candidate |
| Knowledge update timeline | Events linked to a knowledge update application |

Supported filters may include:

- `incidentId`
- `recommendationRecordId`
- `learningCandidateId`
- `knowledgeUpdateApplicationId`
- `from` / `to`
- `eventType`

## Degraded Semantics

Timeline responses may be partial or degraded.

When degraded:

- successful event components should still be returned
- failed components should be listed
- the UI must mark the timeline as partial
- degraded timeline data must remain read-only

## Frontend Compatibility

The timeline query contract should support:

- infinite scroll
- incremental polling
- future WebFlux streaming
- future SSE-based live timeline updates

This phase does not implement streaming.

## Security and Non-goals

Timeline query APIs must not:

- trigger remediation
- approve recommendations
- execute plans
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- update RAG
- update Qdrant
- expose payment payloads
- expose customer data
- expose secrets, tokens, raw logs, or full prompts
