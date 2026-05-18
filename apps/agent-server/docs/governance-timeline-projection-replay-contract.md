# Governance Timeline Projection Replay Contract

## 1. Projection Replay Goals

This document defines the future contract for Governance Timeline projection
replay, rebuild, backfill, and recovery flows.

The goal is to preserve append-only audit semantics, cursor compatibility, and
read-only operator safety while allowing projection state to be rebuilt when
necessary.

## 2. Replay Trigger Scenarios

Replay may be triggered for scenarios such as:

- projection rebuild
- schema migration replay
- projection corruption recovery
- historical backfill
- projection bootstrap

Replay exists to rebuild or restore the read model, not to change governance
history itself.

## 3. Replay Ordering Semantics

Replay behavior must preserve:

- historical ordering compatibility
- `occurredAt DESC, eventId DESC` compatibility
- cursor compatibility
- stable pagination compatibility

Replay strategy may change internal execution order, but the resulting
projected read model must remain compatible with existing timeline query
ordering semantics.

## 4. Replay Idempotency Expectations

Replay behavior should remain idempotent at the event level.

Required expectations:

- `event_id`-based replay idempotency is preserved
- duplicate projection row creation is not allowed
- retry-safe replay is allowed

Repeated replay of the same logical timeline event must not create duplicate
projected audit rows.

## 5. Replay Sanitization Boundary

Replay flows must preserve sanitization guarantees.

Required boundary:

- sanitization remains enforced during replay
- unsafe historical payload storage is not allowed
- secrets, tokens, and passwords must not be stored
- payment data and customer PII must not be stored
- raw prompts and raw responses must not be stored

Replay must not bypass the sanitization rules applied to normal projection
paths.

## 6. Replay Degraded Semantics

Replay flows may degrade under partial failure.

Supported semantics:

- partial replay failure is allowed
- best-effort replay is allowed
- failed replay source isolation remains visible
- external exception and raw stack trace exposure is not allowed

Replay degradation must not silently hide failures or leak unsafe backend
detail.

## 7. Replay Metrics Expectations

Future replay flows should expose observability such as:

- `projection_replay_total`
- `projection_replay_failure_total`
- `projection_replay_degraded_total`

Metrics must preserve low-cardinality discipline.

Metric tags must not include:

- `eventId`
- raw error text
- exception details
- tag explosion from unbounded replay metadata

## 8. Replay Safety Boundaries

Replay is a read-model rebuild mechanism only.

Replay must not:

- trigger governance actions
- trigger remediation
- execute approvals
- mutate GitOps repositories
- mutate Kubernetes
- trigger ArgoCD sync
- update RAG
- update Qdrant

Replay safety boundaries must remain stricter than general operational batch
processing because replay works against historical audit data.

## 9. Migration Expectations

Expected future migration behavior:

- projection schema evolution remains allowed
- projection rebuild remains allowed
- frontend and API compatibility must remain stable
- cursor contract must remain stable

Replay strategy may evolve, but externally visible Timeline query behavior must
remain consistent.

## 10. Non-goals

This contract does not introduce:

- actual replay implementation
- scheduler implementation
- Kafka replay pipeline
- CDC replay
- event sourcing migration
- exactly-once distributed replay
