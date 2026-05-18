# Governance Console Search Contract

## Purpose

This document defines the read-only governance console search contract for the React SRE Console.

Search connects dashboard navigation, lightweight overview preview, and full detail views.

## API

```text
GET /internal/governance/search?q={keyword}&type=all&window=24h&limit=20
```

## Query Parameters

| Parameter | Meaning |
|---|---|
| `q` | Keyword query. Blank values are allowed. |
| `type` | `ALL`, `INCIDENT`, `RECOMMENDATION`, `LEARNING_CANDIDATE`, `KNOWLEDGE_UPDATE` |
| `window` | `1h`, `24h`, `7d` |
| `limit` | Result count, default `20`, max `100` |

## Supported Types

- `ALL`
- `INCIDENT`
- `RECOMMENDATION`
- `LEARNING_CANDIDATE`
- `KNOWLEDGE_UPDATE`

## Result Contract

Each result includes:

- record type
- record id
- title
- status
- sanitized summary
- occurred timestamp
- full detail path
- lightweight overview path

## Read-only Guarantees

Search is read-only.

It does not:

- approve recommendations
- execute plans
- trigger remediation
- modify GitOps state
- update RAG
- update Qdrant

## Non-goals

This phase does not introduce:

- LLM search
- vector search
- Qdrant search
- PostgreSQL full-text search
- ranking model
- mutation workflow

## Sensitive Data Policy

Search responses must not expose:

- metadata
- payload
- rawLog
- payment data
- customer data
- secret values
- token values
- prompt contents
