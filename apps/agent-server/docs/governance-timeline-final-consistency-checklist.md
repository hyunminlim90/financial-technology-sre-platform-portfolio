# Governance Timeline Final Consistency Checklist

## 1. API Surface Consistency

- [ ] `GET /internal/governance/timeline` exists.
- [ ] `GET /internal/governance/timeline/health` exists.
- [ ] `GET /internal/governance/timeline/runtime-summary` exists.
- [ ] Timeline mutation methods are not supported.
- [ ] Response envelope uses `GovernanceTimelineApiResponse`.

## 2. Cursor Semantics Consistency

- [ ] Cursor is opaque.
- [ ] Cursor is Base64 URL-safe.
- [ ] Cursor decode failure returns `INVALID_TIMELINE_CURSOR`.
- [ ] Raw cursor value is never exposed.
- [ ] NEXT returns older events.
- [ ] PREVIOUS returns newer events.

## 3. Ordering Consistency

- [ ] Ordering is `occurredAt DESC, eventId DESC`.
- [ ] Same timestamp uses `eventId` tie-breaker.
- [ ] Cursor pagination preserves stable ordering.

## 4. Projection Mapping Consistency

- [ ] All supported governance sources map to `GovernanceTimelineEvent`.
- [ ] `eventId = {sourceType}:{sourceId}`.
- [ ] Actor, resource, and severity are mapped deterministically.
- [ ] Summary and metadata are sanitized.

## 5. Aggregation and Degradation Consistency

- [ ] Aggregation deduplicates by `eventId`.
- [ ] Degraded aggregation returns successful sources when possible.
- [ ] Failed sources are listed.
- [ ] Exception messages are not exposed.

## 6. Metrics Low-cardinality Consistency

- [ ] Query metrics use low-cardinality tags only.
- [ ] Degraded metrics use low-cardinality source and reason tags only.
- [ ] Page size is recorded as distribution summary.
- [ ] Cursor, eventId, recordId, incidentId, query, exception, and summary are never metric tags.

## 7. Health and Runtime Consistency

- [ ] Timeline health is lightweight.
- [ ] Timeline health does not execute aggregation.
- [ ] Timeline runtime maps health to runtime mode.
- [ ] Timeline runtime gauge is exposed.
- [ ] Console runtime summary includes `timelineRuntime`.

## 8. Console Integration Consistency

- [ ] Console API summary includes Timeline APIs.
- [ ] Frontend integration contract includes timeline runtime rendering.
- [ ] Operator guide documents Timeline query usage.
- [ ] README includes Timeline API usage.

## 9. Security and Read-only Consistency

- [ ] Timeline APIs are internal-only.
- [ ] Timeline APIs are read-only.
- [ ] Timeline APIs are append-only audit views.
- [ ] Timeline APIs do not trigger remediation.
- [ ] Timeline APIs do not mutate Kubernetes, ArgoCD, GitOps, RAG, or Qdrant.

## 10. Non-goals Still Excluded

- [ ] R2DBC optimized timeline query is not introduced.
- [ ] SSE and streaming are not introduced.
- [ ] Kafka is not introduced.
- [ ] DB projection table is not introduced.
- [ ] PrometheusRule and Grafana dashboard are not introduced.
- [ ] React implementation is not introduced.
