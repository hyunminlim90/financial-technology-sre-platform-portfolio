# Governance Timeline Projection Retention Contract

## 1. Projection Retention Goals

This document defines the future contract for Governance Timeline projection
retention, archive, partition, and cleanup behavior.

Retention goals include:

- projection growth management
- query performance preservation
- cold archive compatibility
- long-term audit scalability

## 2. Retention Scope

Projection retention operates within read-model maintenance boundaries only.

This means:

- retention applies to projection storage and related read-model structures
- retention does not imply deletion of source governance records
- retention does not redefine canonical governance history

Projection retention exists to manage read-model scale, not to erase source
audit truth.

## 3. Archive Compatibility

Future projection retention must remain archive-compatible.

Supported expectations:

- cold archive remains allowed
- historical replay compatibility remains preserved
- historical audit continuity remains preserved

Archive movement must not break the logical continuity of the Governance
Timeline audit model.

## 4. Partitioning Expectations

Future projection storage may support:

- `occurred_at`-based partitioning
- append-only compatible partition layout
- ordering-compatible partition strategies

Partitioning is allowed as an implementation strategy, but it must not change
timeline ordering or cursor semantics visible to operators.

## 5. Retention Safety Boundaries

Retention is a read-model maintenance mechanism only.

Retention must not:

- trigger governance actions
- trigger remediation
- execute approvals
- overwrite historical audit state
- perform broad unsafe projection mutation

Projection mutation should remain minimal and constrained to retention
maintenance boundaries.

## 6. Ordering and Cursor Compatibility

Retention and archive behavior must preserve:

- active projection range `occurredAt DESC, eventId DESC` ordering
- cursor stability
- stable pagination compatibility

Retention policies must not silently break cursor navigation or reorder active
timeline history.

## 7. Replay Compatibility

Retention strategy must remain compatible with replay and rebuild flows.

Supported expectations:

- archive and replay may coexist
- historical rebuild compatibility remains preserved
- projection replay compatibility remains preserved

Retention must not make replay unsafe or impossible for supported audit
recovery scenarios.

## 8. Metrics Expectations

Future retention and archive flows should expose observability such as:

- `projection_retention_total`
- `projection_archive_total`
- `projection_retention_failure_total`

Metrics must preserve low-cardinality discipline.

Metric tags must not include:

- `eventId`
- raw archive path detail
- tag explosion from unbounded retention metadata

## 9. Migration Expectations

Expected future migration behavior:

- runtime aggregation may migrate to persistent projection storage
- archive storage evolution remains allowed
- frontend and API compatibility must remain stable
- cursor contract must remain stable

Internal retention and archive strategy may evolve, but externally visible
Timeline behavior must remain consistent.

## 10. Non-goals

This contract does not introduce:

- actual retention scheduler
- partition DDL
- archive storage implementation
- S3 integration
- tiered storage implementation
- automatic legal retention policy
