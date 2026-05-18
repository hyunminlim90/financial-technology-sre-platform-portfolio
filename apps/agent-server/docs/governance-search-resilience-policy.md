# Governance Search Resilience Policy

## Purpose

This document defines resilience policy boundaries for the governance console search API.

The current phase introduces policy and configuration boundaries only.

It does not introduce partial search execution logic yet.

## Search API Principle

The governance search API is read-only.

Search failures must not trigger:

- remediation
- approval
- execution
- GitOps mutation
- RAG update
- Qdrant update

## Partial Search Policy

`ALL` search may support partial results in a future implementation when one search component fails.

Single-type search failure may fail the request.

Partial search responses should be marked degraded when that behavior is introduced.

## Sensitive Query Handling

Search query text `q` must not be used as:

- metric tags
- log correlation keys

Record IDs must not be used as metric tags.

## Scope Boundaries

This phase does not introduce:

- LLM search
- vector search
- Qdrant search
- partial execution logic
- retry or circuit breaker logic
- mutation workflow

## Configuration Defaults

```yaml
agent:
  governance:
    search:
      resilience:
        enabled: false
        partial-search-enabled: true
        fail-open-search: true
        component-query-timeout-ms: 1500
```

Meaning:

- `enabled=false`: resilience behavior is not force-applied yet
- `partial-search-enabled=true`: future partial result behavior is permitted by policy
- `fail-open-search=true`: future search may prefer degraded safe results for `ALL` search
- `component-query-timeout-ms=1500`: baseline timeout boundary for future component query protection
