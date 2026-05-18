# Governance Console Runtime Banner Contract

## Runtime Banner Purpose

This document defines how the React SRE Console runtime banner should render the read-only runtime summary response from:

```text
/internal/governance/console/runtime-summary
```

The runtime banner communicates whether the Governance Console is operating normally, serving degraded read-only data, or requiring operator attention.

The banner is informational only.

## Runtime Mode Display

| Runtime Mode | UI Meaning | Recommended Banner |
|---|---|---|
| `NORMAL` | Console operating normally | Governance Console is operating normally. |
| `DEGRADED_READ_ONLY` | Console available with degraded read-only data | Governance Console is available with degraded read-only data. |
| `ATTENTION_REQUIRED` | Console requires operator attention | Governance Console requires operator attention. |

## Degraded Signals

The banner may render `degradedSignals` as small read-only badges.

Examples:

- `dashboard:DEGRADED`
- `detail:DEGRADED_CAPABLE`
- `search:DEGRADED_CAPABLE`
- `console:ATTENTION_REQUIRED`

These badges are informational only.

## Health Badge Rules

The runtime banner may display sub-health badges for:

- dashboard health
- detail health
- search health

Recommended behavior:

- show dashboard health using `HEALTHY`, `DEGRADED`, `UNAVAILABLE`
- show detail health using `HEALTHY`, `DEGRADED_CAPABLE`, `STRICT`
- show search health using `HEALTHY`, `DEGRADED_CAPABLE`, `STRICT`
- preserve the exact backend status values without UI-side reinterpretation

## Operator-facing Message

The banner should show the `message` field returned by the runtime summary API.

Recommended UI behavior:

- render the backend message as the primary text
- keep degradedSignals secondary
- avoid inventing stronger operational claims than the backend response

## Loading, Empty, and Error States

Recommended handling:

- loading: render a neutral loading banner or skeleton
- empty: treat missing response data as a UI error state
- error: show a read-only console banner error state without suggesting mutation or remediation

The UI must not guess runtime health if the API request fails.

## Read-only Principle

The runtime banner is read-only.

It must not change governance records, search behavior, health configuration, or runtime policy.

## Prohibited Actions

The runtime banner must not provide buttons that:

- approve recommendations
- execute plans
- trigger remediation
- restart services
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- create Git commits or pull requests
- update RAG
- update Qdrant

## Internal-only Console

The runtime banner contract applies only to the internal SRE Console.

The underlying API is internal-only:

```text
/internal/governance/**
```

This contract must not be used as a public product API contract.
