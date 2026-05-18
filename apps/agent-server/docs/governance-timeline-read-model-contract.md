# Governance Timeline Read Model Contract

## Purpose

This document defines the normalized read model for governance timeline events.

Timeline events are immutable read-only audit projections.

This phase defines DTO and contract only.

It does not introduce DB projections, Kafka, SSE, WebSocket, or timeline query implementation.

## Normalized Event Shape

Every timeline event should expose a stable shape:

- `eventId`
- `eventType`
- `occurredAt`
- `title`
- `summary`
- `severity`
- `actor`
- `resource`
- `metadata`
- `degraded`

## Actor Semantics

Supported actor types:

| Actor Type | Meaning |
|---|---|
| `AI` | AI-generated recommendation or draft event |
| `HUMAN` | Human operator decision, review, verification, or application event |
| `SYSTEM` | System-generated lifecycle, persistence, or derived event |

## Resource Semantics

Supported resource types:

- `INCIDENT`
- `RECOMMENDATION`
- `APPROVAL`
- `EXECUTION_PLAN`
- `HUMAN_EXECUTION`
- `VERIFICATION`
- `POSTMORTEM`
- `LEARNING`
- `KNOWLEDGE_PROMOTION`
- `KNOWLEDGE_UPDATE`

## Severity Semantics

Recommended severity mapping:

| Severity | Meaning |
|---|---|
| `INFO` | Normal governance event |
| `WARNING` | Needs review, degraded, or pending human action |
| `ERROR` | Failed verification, rejection, or blocked governance step |
| `CRITICAL` | Security or payment integrity critical signal |

## Immutability and Append-only Semantics

Timeline events are immutable read-only audit projections.

Timeline events must not be mutated in place.

If new context appears, append a new event instead of modifying an existing event.

## Sanitization

Timeline summaries and metadata must not expose:

- payment payloads
- customer data
- secrets
- tokens
- passwords
- raw logs
- full prompts

Metadata must be sanitized low-risk key-value attributes only.

## Streaming Compatibility

The timeline event shape should remain stable for future:

- cursor pagination
- incremental polling
- WebFlux streaming
- SSE-based live incident timeline updates

This contract does not implement streaming.

## Non-goals

This contract does not introduce:

- DB projection tables
- timeline query implementation
- Kafka events
- SSE
- WebSocket
- remediation actions
- GitOps mutation
- RAG ingestion
- Qdrant updates
