# Governance Timeline Projection Failure Taxonomy Contract

## 1. Projection Failure Taxonomy Goals

This document defines future failure taxonomy semantics for Governance Timeline
projection persistence and projection-backed runtime behavior.

The goals of this taxonomy are:

- operator-facing informational taxonomy
- best-effort degraded semantics preservation
- partial degraded projection support
- prevention of raw exception detail exposure

## 2. Failure Classification Principles

Projection failure classification should follow these principles:

- taxonomy is operator-facing and informational
- best-effort degraded semantics should remain preserved
- partial degraded projection remains allowed
- raw exception detail must not be exposed externally
- low-cardinality classification is preferred over ad hoc error wording

The taxonomy exists to standardize operator understanding, not to encode
automated mutation behavior.

## 3. Projection Write Failure Categories

Recommended projection write failure categories include:

- `projection_write_failure`
- `projection_write_partial_failure`
- `projection_write_degraded`

These categories distinguish complete write failure, partial source write
failure, and degraded but still usable projection write conditions.

## 4. Replay Failure Categories

Recommended replay failure categories include:

- `projection_replay_failure`
- `projection_replay_partial_failure`
- `projection_replay_degraded`

These categories distinguish replay failure, partial replay completion, and
degraded replay states that still preserve best-effort operator visibility.

## 5. Bootstrap Failure Categories

Recommended bootstrap failure categories include:

- `projection_bootstrap_failure`
- `projection_bootstrap_partial_failure`
- `projection_bootstrap_degraded`

Bootstrap taxonomy should help operators understand whether initialization
fully failed, partially failed, or produced a degraded projection state.

## 6. Retention Failure Categories

Recommended retention failure categories include:

- `projection_retention_failure`
- `projection_retention_partial_failure`
- `projection_archive_degraded`

Retention taxonomy should distinguish retention execution failure, partial
retention behavior, and degraded archive or archive-adjacent maintenance state.

## 7. Consistency Degradation Categories

Recommended consistency degradation categories include:

- `projection_consistency_degraded`
- `projection_ordering_drift`
- `projection_cursor_consistency_degraded`
- `projection_partial_availability`

These categories exist to expose operator-visible consistency drift without
requiring raw backend detail.

## 8. Operator-facing Failure Visibility

Projection failure taxonomy is operator-facing informational semantics only.

Projection failure taxonomy must not imply:

- auto-remediation semantics
- governance action trigger semantics
- approval execution semantics
- remediation execution semantics

Projection failure visibility should support safe diagnosis, degraded-state
awareness, and human investigation only.

Degraded semantics that must remain supported:

- partial degraded projection remains allowed
- failed source isolation remains visible
- best-effort degraded availability remains preserved

## 9. Metrics Expectations

Projection failure observability should support metrics such as:

- `projection_failure_total`
- `projection_degraded_total`
- `projection_partial_availability_total`

All projection failure metrics must preserve low-cardinality discipline.

Metric tags must not include:

- `eventId`
- raw exception detail
- unbounded tags that cause tag explosion

## 10. Non-goals

This contract does not introduce:

- automatic remediation mapping
- distributed incident orchestration
- alert routing implementation
- cross-system failure coordination
