# Governance Timeline Projection Observability Contract

## 1. Projection Observability Goals

This document defines future observability semantics for Governance Timeline
projection persistence and projection runtime operations.

Projection observability goals include:

- projection runtime visibility
- projection degraded visibility
- projection replay visibility
- projection retention visibility
- operator-facing observability

## 2. Projection Runtime Visibility

Projection runtime observability should provide visibility into:

- projection write visibility
- projection replay visibility
- projection retention visibility
- projection lag visibility where future persistence implementations support it

Projection observability should help operators understand whether the
projection-backed read model is current, degraded, or delayed.

## 3. Projection Health Visibility

Projection observability should provide visibility into:

- projection degraded state visibility
- best-effort availability visibility
- partial failure visibility
- projection write failure visibility

Projection health visibility is intended to expose read-model conditions
without triggering operational side effects.

## 4. Replay Visibility

Replay observability should provide visibility into:

- replay execution visibility
- replay degraded visibility
- historical rebuild visibility

Replay observability is informational and should help operators distinguish
normal replay progress from degraded replay conditions.

## 5. Retention Visibility

Retention observability should provide visibility into:

- retention execution visibility
- archive visibility
- partition maintenance visibility where future projection stores use
  partitioned maintenance paths

Retention visibility is intended to help operators understand read-model scale
management, not to expose raw storage internals.

## 6. Metrics Expectations

Future projection observability should support metrics such as:

- `projection_write_total`
- `projection_write_failure_total`
- `projection_write_degraded_total`
- `projection_replay_total`
- `projection_replay_failure_total`
- `projection_retention_total`
- `projection_retention_failure_total`
- `projection_degraded_total`

All projection observability must preserve low-cardinality metric discipline.

Metric tags must not include:

- `eventId`
- raw exception detail
- unbounded tag sets that cause tag explosion

## 7. Degraded Projection Visibility

Projection observability should make the following visible:

- partial degraded projection states
- failed source isolation
- best-effort degraded read availability

Projection observability should allow operators to see degraded projection
conditions without exposing unsafe backend detail.

## 8. Operator-facing Semantics

Projection observability is operator-facing informational semantics only.

Projection observability must not imply:

- auto-remediation semantics
- governance action trigger semantics
- approval trigger semantics
- remediation trigger semantics

Projection observability is for awareness, diagnosis, and safe runtime status
interpretation only.

## 9. Migration Expectations

Expected future migration behavior:

- runtime aggregation to projection persistence migration compatibility remains
  supported
- metrics continuity should remain preserved across migration
- frontend and runtime compatibility should remain preserved

Internal implementation and storage strategies may evolve, but projection
observability semantics should remain stable enough for operator interpretation.

## 10. Non-goals

This contract does not introduce:

- actual Grafana dashboard
- Prometheus scrape config
- OpenTelemetry pipeline
- alerting implementation
- auto-remediation
- SRE runbook automation
