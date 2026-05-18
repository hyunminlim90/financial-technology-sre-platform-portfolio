# React Governance Timeline Rendering Contract

## 1. Timeline Row Layout

Each timeline row should render the following informational elements in a stable order:

- timestamp
- severity badge
- event type label
- title
- summary
- actor
- resource
- degraded badge when applicable

The row is an audit rendering unit, not an action surface.

## 2. Severity Badge Rendering

Severity rendering semantics:

- `INFO` -> neutral informational state
- `WARNING` -> degraded, review, or attention-required state
- `ERROR` -> failed, rejected, or blocked state
- `CRITICAL` -> payment, security, or system-critical state

Severity badges must remain informational only.

## 3. Event Type Label Rendering

The event type label should preserve backend event meaning without UI-side reinterpretation.

Recommended examples:

- `INCIDENT_TRANSITIONED`
- `VERIFICATION_RECORDED`
- `POSTMORTEM_REVIEWED`

Labels may be humanized for display, but the semantic mapping must remain stable.

## 4. Actor and Resource Rendering

Actor and resource fields are informational rendering only.

They may be displayed as contextual labels, chips, or secondary metadata.

They must not become:

- remediation controls
- execution controls
- hidden mutation links

## 5. Timestamp Rendering

Timestamps should be rendered as operator-readable time values.

Recommended behavior:

- show localized human-readable time
- preserve machine-readable timestamp metadata for inspection or accessibility

## 6. Degraded Event Rendering

If an individual event or response context is degraded:

- render an informational degraded badge
- keep successful event content visible
- avoid treating degraded event rendering as fatal UI failure

## 7. Partial Timeline Banner Rendering

If the timeline response is partial or degraded:

- render a top-level partial timeline banner
- display degraded signals or failed component information when available
- keep the timeline panel usable

Partial degraded timeline is not a fatal UI state.

## 8. Empty, Loading, and Error Rendering

Required rendering semantics:

- empty timeline: show a neutral empty state
- loading initial page: show loading skeletons or placeholders
- loading older events: show an inline older-page loading state
- loading newer events: show an inline newer-page loading state
- invalid cursor recovery: show a safe reset or reload state
- retry after 5xx: show a read-only retry candidate error state

## 9. Accessibility Baseline

Minimum accessibility expectations:

- severity badge should not rely on color only
- timestamp should be machine-readable
- loading state should be screen-reader compatible
- degraded or error disclosure should be textually explicit

## 10. Forbidden Action Controls

The timeline rendering surface must not provide:

- approve button
- execute button
- remediation button
- kubectl action
- ArgoCD action
- GitOps mutation control
- RAG mutation control
- Qdrant mutation control

## 11. Non-goals

This contract does not introduce:

- React JSX implementation
- Tailwind or CSS implementation
- component library selection
- virtualized list implementation
- real rendering code
