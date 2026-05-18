# Governance Timeline Implementation Readiness Checklist

## Purpose

This document defines implementation readiness criteria before building the actual governance timeline query and aggregation flow.

The goal is to ensure contract-first alignment, scope control, and safe implementation boundaries.

## Contract Dependency Checklist

- [ ] `docs/governance-timeline-pagination-contract.md`
- [ ] `docs/governance-timeline-query-contract.md`
- [ ] `docs/governance-timeline-read-model-contract.md`
- [ ] `docs/governance-timeline-mapping-contract.md`
- [ ] `docs/governance-timeline-aggregation-contract.md`
- [ ] `docs/governance-timeline-resilience-contract.md`
- [ ] `docs/governance-timeline-metrics-contract.md`
- [ ] `docs/governance-timeline-health-contract.md`
- [ ] `docs/governance-timeline-runtime-contract.md`
- [ ] `docs/governance-timeline-frontend-integration-contract.md`
- [ ] `docs/governance-timeline-api-contract.md`

## DTO Stability Checklist

- [ ] timeline event taxonomy is fixed
- [ ] normalized timeline read model shape is fixed
- [ ] projection envelope is fixed
- [ ] aggregation envelope is fixed
- [ ] degradation envelope is fixed
- [ ] API response envelope is fixed
- [ ] health and runtime DTO shape is fixed

## Store and Query Readiness Checklist

- [ ] required source stores are available
- [ ] incident-scoped source lookup path is defined
- [ ] recommendation-scoped source lookup path is defined
- [ ] learning-scoped source lookup path is defined
- [ ] knowledge-update-scoped source lookup path is defined
- [ ] future cursor query boundary is defined
- [ ] offset pagination remains out of scope

## Projection Mapper Readiness

- [ ] `RecommendationRecord` → `RECOMMENDATION_CREATED`
- [ ] `RecommendationApprovalRecord` → `APPROVAL_DECIDED`
- [ ] `RecommendationExecutionPlan` → `EXECUTION_PLAN_CREATED`
- [ ] `HumanExecutionResultRecord` → `HUMAN_EXECUTION_RECORDED`
- [ ] `VerificationResultRecord` → `VERIFICATION_RECORDED`
- [ ] `IncidentLifecycleRecord` → `INCIDENT_TRANSITIONED`
- [ ] `PostmortemDraftRecord` → `POSTMORTEM_DRAFT_CREATED`
- [ ] `PostmortemReviewRecord` → `POSTMORTEM_REVIEWED`
- [ ] `LearningCandidateRecord` → `LEARNING_CANDIDATE_CREATED`
- [ ] `KnowledgePromotionReviewRecord` → `PROMOTION_REVIEWED`
- [ ] `KnowledgePromotionPlanRecord` → `PROMOTION_PLAN_CREATED`
- [ ] `KnowledgeUpdateApplicationRecord` → `KNOWLEDGE_UPDATED`

## Cursor Encoding Readiness

- [ ] cursor identity uses `occurredAt`, `eventType`, `eventId`
- [ ] cursor remains opaque to the frontend
- [ ] stable ordering uses `occurredAt DESC, eventId DESC`
- [ ] deterministic tie-breaker strategy is fixed
- [ ] filter changes invalidate existing cursors

## Sanitization Readiness

- [ ] timeline summary sanitization policy is defined
- [ ] timeline metadata sanitization policy is defined
- [ ] payment payload exclusion is defined
- [ ] customer data exclusion is defined
- [ ] secret and token exclusion is defined
- [ ] raw log and prompt exclusion is defined

## Resilience Readiness

- [ ] `STRICT` mode semantics are defined
- [ ] `PARTIAL_DEGRADED` mode semantics are defined
- [ ] `FAIL_OPEN_READ_ONLY` mode semantics are defined
- [ ] failed source reporting shape is defined
- [ ] low-cardinality degraded reason taxonomy is defined
- [ ] partial timeline disclosure requirement is defined

## Metrics Readiness

- [ ] query metric naming is defined
- [ ] aggregation metric naming is defined
- [ ] degraded metric naming is defined
- [ ] page size metric naming is defined
- [ ] health metric naming is defined
- [ ] low-cardinality tag rules are defined
- [ ] forbidden high-cardinality tag values are defined

## Frontend Compatibility Readiness

- [ ] timeline panel usage is defined
- [ ] infinite scroll guidance is defined
- [ ] degraded rendering behavior is defined
- [ ] actor and resource rendering guidance is defined
- [ ] severity rendering guidance is defined
- [ ] incremental polling compatibility is defined
- [ ] internal-only UI assumptions are defined

## Implementation Phase Boundaries

Phase 1: in-memory timeline aggregation from existing stores

Phase 2: R2DBC optimized timeline query

Phase 3: cursor pagination implementation

Phase 4: metrics and health wiring

Phase 5: frontend integration

Phase 6: future streaming readiness

## Non-goals

- Kafka
- SSE
- WebSocket
- automatic remediation
- GitOps mutation
- RAG ingestion
- Qdrant update
- DB projection table
