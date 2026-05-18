# Governance Timeline Projection Recovery Contract

## 1. Projection Recovery Goals

This document defines future recovery semantics for Governance Timeline
projection persistence and projection-backed runtime behavior.

Projection recovery goals include:

- safe recovery from projection corruption
- safe recovery from projection lag
- best-effort degraded recovery support
- operator-facing recovery visibility

## 2. Recovery Trigger Scenarios

Recovery may be triggered for scenarios such as:

- projection corruption
- projection lag
- partial projection write failure
- projection replay rebuild
- projection bootstrap recovery

Recovery exists to restore the read model, not to alter governance history or
trigger operational actions.

## 3. Recovery Safety Boundaries

Recovery is a read-model recovery mechanism only.

Recovery must not:

- trigger governance actions
- execute approvals
- trigger remediation
- mutate GitOps repositories
- mutate Kubernetes
- trigger ArgoCD sync
- update RAG
- update Qdrant

Recovery safety boundaries must remain stricter than general background
processing because recovery acts on degraded or inconsistent read-model state.

## 4. Replay-based Recovery Semantics

Replay-based recovery is allowed when rebuilding or restoring projection state.

Required semantics:

- replay-based rebuild remains allowed
- idempotent recovery remains preserved
- ordering compatibility remains preserved
- cursor compatibility remains preserved
- stable pagination compatibility remains preserved

Recovery may use replay internally, but externally visible Timeline semantics
must remain stable.

## 5. Degraded Recovery Semantics

Recovery may degrade under partial failure.

Supported semantics:

- best-effort degraded recovery is allowed
- partial recovery visibility remains preserved
- failed recovery source isolation remains preserved
- recovery degradation visibility remains preserved

Degraded recovery must remain visible to operators and must not silently hide
unsafe or incomplete recovery state.

## 6. Recovery Consistency Expectations

Recovery behavior must preserve:

- ordering consistency after recovery
- `event_id` dedup consistency
- replay consistency
- retention compatibility

Recovery exists to restore a projection-backed timeline that remains compatible
with existing query, replay, and retention expectations.

## 7. Recovery Observability Expectations

Future recovery flows should expose observability such as:

- `projection_recovery_total`
- `projection_recovery_failure_total`
- `projection_recovery_degraded_total`

All recovery observability must preserve low-cardinality metric discipline.

Metric tags must not include:

- `eventId`
- raw exception detail
- unbounded tags that cause tag explosion

## 8. Operator-facing Recovery Visibility

Recovery observability is operator-facing informational semantics only.

Recovery visibility must not imply:

- auto-remediation semantics
- governance action trigger semantics

Recovery visibility exists to help operators understand projection health,
rebuild progress, degraded recovery state, and safe follow-up actions.

## 9. Migration Expectations

Expected future migration behavior:

- runtime aggregation to persistent projection migration compatibility remains
  preserved
- projection replay and rebuild compatibility remains preserved
- frontend and API compatibility remains preserved
- cursor contract remains preserved

Internal storage and recovery strategies may evolve, but externally visible
Timeline recovery semantics should remain stable.

## 10. Non-goals

This contract does not introduce:

- automatic remediation orchestration
- distributed recovery coordinator
- cross-region recovery
- event sourcing migration
- exactly-once recovery guarantee
