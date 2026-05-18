# Governance Timeline Resilience Policy

## Purpose

This document defines resilience policy and configuration boundaries for governance timeline query and aggregation behavior.

The governance timeline is a read-only operational audit view.

This phase introduces policy and configuration only.

It does not introduce timeout enforcement, parallel source isolation, circuit breakers, R2DBC query timeout wiring, or streaming timeout behavior.

## Timeline API Principle

Timeline APIs are read-only.

Timeline failures must not trigger:

- remediation
- approval
- execution
- Kubernetes mutation
- ArgoCD mutation
- GitOps mutation
- RAG update
- Qdrant update

## Partial Degraded Timeline Policy

When resilience is enabled, timeline aggregation may support partial degraded read-only responses.

Meaning:

- successful timeline components may still be returned
- failed components may be disclosed through low-cardinality degraded metadata
- degraded timelines remain safe to render as read-only audit data

## Fail-open Read-only Policy

`fail-open-read-only=true` allows the timeline layer to prefer degraded read-only availability over full failure when partial degraded timeline behavior is enabled.

If `fail-open-read-only=false`, the timeline remains strict even if partial degraded semantics are modeled.

## Timeout Configuration Boundary

`component-query-timeout-ms` defines the baseline timeout boundary for future component query protection.

This phase does not yet enforce the timeout in execution logic.

## Sensitive Data and Tag Boundaries

Timeline resilience policy must not expose or use the following as metric or log tags:

- cursor
- query text
- raw exception message
- eventId
- recordId
- incidentId
- recommendationRecordId
- learningCandidateId
- knowledgeUpdateApplicationId

## Configuration Defaults

```yaml
agent:
  governance:
    timeline:
      resilience:
        enabled: false
        partial-timeline-enabled: true
        fail-open-read-only: true
        component-query-timeout-ms: 1500
```

Meaning:

- `enabled=false`: timeline resilience behavior is not force-applied yet
- `partial-timeline-enabled=true`: partial degraded timeline behavior is permitted by policy
- `fail-open-read-only=true`: degraded read-only timeline availability is permitted by policy
- `component-query-timeout-ms=1500`: baseline timeout boundary for future component query protection
