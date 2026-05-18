# Governance Detail Query Resilience Policy

## Purpose

This document defines resilience policy boundaries for governance detail query APIs.

Governance detail APIs are read-only aggregate views built from append-only governance records.

The current phase defines policy and configuration boundaries only.

Automatic retry, circuit breaker behavior, cache, mutation, or degraded detail execution flow is not introduced yet.

## Scope

Current detail query scope includes:

- incident detail
- recommendation detail
- learning candidate detail
- knowledge update detail

## Primary Record Rule

If the primary record for a detail request does not exist, the API should return `404 Not Found`.

Examples:

- missing incident lifecycle root context for an incident detail lookup
- missing recommendation record for a recommendation detail lookup
- missing learning candidate for a learning detail lookup
- missing knowledge update application for a knowledge update detail lookup

## Subordinate Component Failure Rule

Subordinate record queries may fail independently from the primary record query.

Examples:

- promotion review history query failure
- verification history query failure
- postmortem review query failure
- knowledge update lookup query failure

These failures must not trigger:

- remediation
- approval
- execution
- rollback
- GitOps mutation
- RAG ingestion
- Qdrant update

## Partial Response Rule

When enabled in a future implementation, detail APIs may return partial read-only responses if:

- the primary record exists
- subordinate record queries fail
- fail-open detail mode is enabled
- partial response mode is enabled

Partial detail responses should be marked as degraded in the response contract when that behavior is implemented.

## Read-only Principle

Governance detail APIs are read-only operational views.

Query failure or degraded detail behavior must not mutate operational audit history.

## Configuration Defaults

Current default configuration:

```yaml
agent:
  governance:
    detail:
      resilience:
        enabled: false
        fail-open-detail: true
        partial-response-enabled: true
        component-query-timeout-ms: 1500
```

Meaning:

- `enabled=false`: resilience behavior is not force-applied yet
- `fail-open-detail=true`: future detail APIs may prefer degraded read-only responses
- `partial-response-enabled=true`: future partial detail responses are allowed by policy
- `component-query-timeout-ms=1500`: baseline timeout boundary for future component query protection

## Operational Notes

Detail query resilience degradation is an observability and availability concern.

It is not an execution signal and must not be interpreted as approval to bypass human review.

## Non-goals

This policy does not introduce:

- automatic retry
- circuit breaker behavior
- cache or materialized view
- mutation API
- remediation action
- approval execution
- Git repository mutation
- RAG ingestion
- Qdrant update
