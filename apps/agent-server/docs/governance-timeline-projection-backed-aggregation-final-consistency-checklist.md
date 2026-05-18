# Governance Timeline Projection-backed Aggregation Final Consistency Checklist

## 1. Projection Write Path Consistency

- [ ] `GovernanceTimelineProjection` maps to `GovernanceTimelineProjectionRecord`.
- [ ] `GovernanceTimelineProjectionWriter` delegates append to `GovernanceTimelineProjectionStore`.
- [ ] `eventId`-based idempotency is preserved.
- [ ] Duplicate writes return `DUPLICATE_SKIPPED`.
- [ ] Writer metrics use low-cardinality tags only.

## 2. Projection Query Path Consistency

- [ ] `GovernanceTimelineProjectionStore.findRecent(...)` feeds the projection query adapter.
- [ ] Projection records are restored into timeline page items.
- [ ] `eventType` filtering is supported.
- [ ] Inclusive `from` and `to` filtering is supported.
- [ ] Empty projection store returns an empty page.

## 3. Ordering and Cursor Semantics Consistency

- [ ] Ordering remains `occurredAt DESC, eventId DESC`.
- [ ] `NEXT` returns older events.
- [ ] `PREVIOUS` returns newer events.
- [ ] Same timestamp uses `eventId` tie-breaker.
- [ ] Invalid cursor fails with `GovernanceTimelineCursorDecodeException`.
- [ ] `nextCursor` and `previousCursor` remain opaque.

## 4. Metrics Low-cardinality Consistency

- [ ] Projection writer metrics expose write result only.
- [ ] Projection query metrics expose result and direction only.
- [ ] Page size is recorded as a distribution summary.
- [ ] `cursor`, `eventId`, `sourceId`, `incidentId`, `exceptionMessage`, `summary`, and `metadata` are never metric tags.

## 5. Health and Runtime Summary Consistency

- [ ] Projection query health remains lightweight.
- [ ] Projection query health does not execute actual queries.
- [ ] Projection query runtime summary only composes health.
- [ ] Runtime summary remains operator-facing informational only.

## 6. Aggregation Routing Compatibility

- [ ] `RUNTIME_FAN_OUT` remains the default.
- [ ] `PROJECTION_BACKED` remains explicit future mode.
- [ ] Routing service can delegate by aggregation mode.
- [ ] Controller wiring is not changed by this phase.

## 7. Runtime Fan-out Compatibility

- [ ] Existing runtime fan-out aggregation remains untouched.
- [ ] Existing timeline API contract remains unchanged.
- [ ] Projection-backed path can be rolled back by returning to runtime fan-out mode.

## 8. Read-only Governance Boundary Consistency

- [ ] Projection-backed path remains read-model only.
- [ ] Projection-backed query path remains read-only.
- [ ] No approval execution is introduced.
- [ ] No remediation trigger is introduced.
- [ ] No GitOps, ArgoCD, Kubernetes, RAG, or Qdrant mutation is introduced.

## 9. Future R2DBC Migration Compatibility

- [ ] Projection store interface can be backed by a future R2DBC implementation.
- [ ] API, cursor, and frontend contracts remain stable.
- [ ] In-memory projection store remains a test and local implementation only.

## 10. Non-goals Consistency

- [ ] No controller wiring switch is introduced.
- [ ] No `@Primary` activation is introduced.
- [ ] No R2DBC repository is introduced.
- [ ] No PostgreSQL DDL is introduced.
- [ ] No production projection-backed activation is introduced.
