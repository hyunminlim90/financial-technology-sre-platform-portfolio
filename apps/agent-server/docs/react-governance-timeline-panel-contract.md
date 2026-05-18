# React Governance Timeline Panel Contract

## Timeline Panel Purpose

The React Governance Timeline panel renders an internal, read-only, append-only operational audit view for governance events.

The panel is intended for operator awareness, audit review, and timeline navigation.

It must not expose operational mutation controls.

## Required Backend APIs

The panel depends on:

- `GET /internal/governance/timeline/runtime-summary`
- `GET /internal/governance/timeline`
- `GET /internal/governance/timeline/health`

## Initial Load Flow

Recommended load sequence:

1. Fetch `/internal/governance/timeline/runtime-summary`
2. Fetch `/internal/governance/timeline?limit=50`
3. Render the timeline panel with runtime badge and initial page

The runtime summary should be treated as lightweight and informational.

## Cursor Pagination UX

Timeline pagination must use backend cursor values as opaque transport tokens.

Allowed behavior:

- use `nextCursor` for older events
- use `previousCursor` for newer events
- preserve `limit`
- preserve active filters

Forbidden behavior:

- parse cursor contents
- infer IDs from cursors
- mutate cursor values
- expose cursor contents in UI diagnostics

## NEXT / PREVIOUS Handling

Timeline ordering is:

```text
occurredAt DESC, eventId DESC
```

UI interpretation:

- `NEXT` loads older events
- `PREVIOUS` loads newer events

Recommended panel flow:

1. initial page from `/internal/governance/timeline?limit=50`
2. use `nextCursor` to append older events
3. use `previousCursor` to refresh or prepend newer events

## Degraded Timeline Rendering

If timeline response metadata or runtime summary indicates degraded behavior, the panel must:

- show a degraded or partial timeline disclosure
- show failed component or degraded signal information when available
- keep successful events visible
- avoid presenting degraded data as complete

Degraded timeline responses remain read-only and informational only.

## Event Severity Rendering

Recommended severity rendering:

| Severity | UI Treatment |
|---|---|
| `INFO` | Normal audit event |
| `WARNING` | Review-needed, pending, or degraded event |
| `ERROR` | Failed, rejected, or blocked governance event |
| `CRITICAL` | Security or payment integrity critical signal |

Severity must not trigger automatic actions.

## Actor and Resource Rendering

Timeline events may include:

- actor
- resource
- eventType
- severity
- occurredAt
- title
- summary

Actor and resource should be rendered as contextual labels, not as executable controls.

## Empty, Loading, and Error States

Recommended handling:

- loading: render timeline skeleton rows
- empty: show `No governance timeline events found.`
- error: show a read-only panel error state

The panel must not suggest remediation or operational action from error states.

## Read-only Guarantees

The timeline panel is read-only.

It must not:

- approve recommendations
- execute plans
- trigger remediation
- mutate Kubernetes
- mutate ArgoCD
- modify GitOps repositories
- update RAG
- update Qdrant

## Forbidden Actions

The panel must not provide:

- approve button
- execute button
- remediate button
- kubectl action
- ArgoCD action
- GitOps action
- RAG action
- Qdrant action
