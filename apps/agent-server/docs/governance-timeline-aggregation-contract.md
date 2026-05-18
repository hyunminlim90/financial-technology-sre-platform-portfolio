# Governance Timeline Aggregation Contract

## Purpose

This document defines how governance source records should be aggregated into a normalized, cursor-pageable timeline.

Timeline aggregation is a read-only projection flow.

This phase defines contract only.

It does not introduce actual aggregation services, DB queries, Kafka, SSE, WebSocket, or streaming.

## Aggregation Pipeline

Recommended pipeline:

```text
source records
→ projection
→ sanitization
→ merge
→ deduplication
→ stable ordering
→ cursor page response
```

Aggregation must not mutate source records.

## Source Coverage

Supported aggregation sources:

- `RECOMMENDATION`
- `APPROVAL`
- `EXECUTION_PLAN`
- `HUMAN_EXECUTION`
- `VERIFICATION`
- `INCIDENT_LIFECYCLE`
- `POSTMORTEM_DRAFT`
- `POSTMORTEM_REVIEW`
- `LEARNING_CANDIDATE`
- `KNOWLEDGE_PROMOTION_REVIEW`
- `KNOWLEDGE_PROMOTION_PLAN`
- `KNOWLEDGE_UPDATE`

## Stable Ordering

Aggregated timeline events must use deterministic ordering:

```text
occurredAt DESC, eventId DESC
```

The same source records should produce the same merged timeline order.

## Deduplication

Aggregation should avoid duplicate events generated from identical source projections.

Recommended deduplication key:

```text
eventId
```

If duplicate projections are detected, the aggregation layer should keep one deterministic event and avoid rendering duplicates.

## Degraded Aggregation

Aggregation may be degraded when one or more source components fail.

When degraded:

- successful source projections may still be returned
- failedSources must be listed
- reason must be low-cardinality
- exception messages must not be exposed
- the UI must mark the timeline as partial

## Consistency Semantics

Timeline aggregation is best-effort consistent under concurrent append-only inserts.

The implementation should prefer:

```text
duplicate avoidance > perfect snapshot isolation
```

A newly appended event may appear on refresh or a later page.

## Security and Sanitization

Aggregation output must not expose:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts

All summaries and metadata must be sanitized before merge.

## Non-goals

This contract does not introduce:

- actual aggregation service implementation
- DB cursor queries
- projection tables
- Kafka events
- WebFlux streaming
- SSE
- WebSocket
- mutation APIs
- remediation
- GitOps mutation
- RAG ingestion
- Qdrant updates
