# Governance Timeline R2DBC Projection Schema Contract

## 1. Projection Table Purpose

This document defines the future schema contract for a PostgreSQL and R2DBC
backed Governance Timeline projection table.

The goal is to preserve the current Timeline API and cursor behavior while
describing a future materialized projection shape that is optimized for
read-only timeline queries.

## 2. Recommended Projection Table Shape

Recommended future shape:

- one append-only projection row per timeline event
- one read-optimized table or equivalent projection structure
- one stable event identity per projected timeline row
- one schema oriented toward pagination, filtering, and audit retention

This document defines schema shape expectations only. It does not create the
actual table.

## 3. Required Columns

Recommended required columns:

- `event_id`
- `event_type`
- `occurred_at`
- `source_type`
- `source_id`
- `incident_id`
- `recommendation_record_id`
- `learning_candidate_id`
- `knowledge_update_application_id`
- `severity`
- `actor_type`
- `resource_type`
- `title`
- `summary`
- `metadata_json`
- `degraded`
- `created_at`

These columns represent the minimum stable shape needed for timeline
pagination, filtering, and audit-oriented rendering.

## 4. JSONB Metadata Policy

`metadata_json` should contain sanitized JSONB only.

Required policy:

- sanitized metadata only
- low-risk read-model attributes only
- arbitrary raw payload storage is not allowed
- raw internal exception detail is not allowed

The projection schema must not become a transport path for unsafe source
payloads.

## 5. Recommended Index Strategy

Recommended indexes include:

- `(occurred_at DESC, event_id DESC)`
- `(event_type, occurred_at DESC)`
- `(incident_id, occurred_at DESC)`
- `(recommendation_record_id, occurred_at DESC)`
- `GIN(metadata_json)`

Additional indexes may be introduced later, but these recommended indexes align
with the current Timeline contract and expected query paths.

## 6. Ordering and Cursor Compatibility

Future projection schema and query implementations must preserve:

- `occurredAt DESC, eventId DESC` ordering
- opaque cursor compatibility
- stable pagination behavior
- `NEXT` older-events semantics
- `PREVIOUS` newer-events semantics

Schema evolution must not break cursor compatibility at the API contract level.

## 7. Append-only Projection Semantics

Projection rows are expected to be append-only.

This means:

- projection row append-only behavior is preserved
- historical audit mutation is not allowed
- `event_id` uniqueness is preserved
- newly discovered context should be represented through new rows rather than
  in-place audit history mutation

The projection schema exists to preserve audit history, not to rewrite it.

## 8. Sanitization and Sensitive Data Boundary

The projection schema must not store unsafe sensitive data.

Forbidden data classes include:

- payment information
- customer PII
- secrets
- tokens
- passwords
- raw prompts
- raw LLM responses
- stack traces
- raw logs

The `summary` and `metadata_json` fields must remain sanitized and safe for
internal read-only operator views.

## 9. Retention and Archive Compatibility

Future projection storage may support:

- cold archive flows
- partitioning strategies
- retention policy enforcement

These optimizations must remain compatible with append-only audit semantics and
must not break cursor ordering behavior for active query ranges.

## 10. Migration Expectations

Expected future migration behavior:

- runtime aggregation may migrate to an R2DBC projection-backed query path
- frontend and API compatibility must remain stable
- cursor contract must remain stable
- degraded response semantics must remain stable

Migration may change internal storage or indexing strategy, but it must not
change the externally visible Governance Timeline contract.

## 11. Non-goals

This contract does not introduce:

- actual PostgreSQL DDL
- Flyway migration
- Liquibase migration
- R2DBC repository
- projection writer
- CDC pipeline
- Kafka
- Debezium
- SSE
- WebSocket
