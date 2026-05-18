# React Governance Timeline Interaction Contract

## 1. Allowed Timeline Interactions

The timeline UI may allow only read-only, navigational, and recovery-oriented interactions.

Allowed interactions:

- load older events
- load newer events
- refresh timeline
- apply eventType filter
- clear filters
- open related detail or overview page
- retry after 5xx
- reset cursor after invalid cursor
- dismiss informational degraded banner

## 2. Pagination Interactions

Timeline pagination semantics:

- `NEXT` -> older events
- `PREVIOUS` -> newer events

Cursor values are opaque navigation tokens.

Allowed pagination behavior:

- request older events with `nextCursor`
- request newer events with `previousCursor`
- preserve backend ordering semantics

## 3. Filter Interactions

Allowed filter interactions:

- apply event type filter
- clear event type filter
- preserve active filters while paginating

Filters must remain read-only query parameters.

## 4. Runtime Banner Interactions

Runtime banner interactions are informational only.

Allowed behavior:

- observe runtime mode
- observe degraded signals
- dismiss non-blocking informational banners if the UI supports dismissal

The runtime banner must not expose mutation controls.

## 5. Error Recovery Interactions

### Invalid Cursor Recovery

`INVALID_TIMELINE_CURSOR` is a recoverable UI state.

Allowed recovery behavior:

- reset pagination state
- reload initial page
- preserve filters when safe

### Retry Semantics

`TIMELINE_QUERY_FAILED`:

- retry allowed
- may be treated as retry candidate

`INVALID_TIMELINE_QUERY`:

- retry not recommended
- should be handled as invalid filter or invalid query state

## 6. Detail Navigation Interactions

Timeline row click or equivalent interaction may:

- open read-only governance detail page
- open read-only governance overview preview
- navigate to related incident, recommendation, learning, or knowledge update context

Timeline interaction is navigation only.

It must not trigger actions.

## 7. Degraded Timeline Interactions

Degraded timeline remains navigable.

Partial degraded banner behavior:

- informational only
- not a blocking modal state
- should not prevent pagination or detail navigation

## 8. Forbidden Mutating Interactions

The timeline interaction surface must not allow:

- approve recommendation
- execute plan
- trigger remediation
- restart service
- kubectl action
- ArgoCD sync
- GitOps mutation
- RAG update
- Qdrant ingestion

## 9. Accessibility Interaction Baseline

Minimum expectations:

- keyboard-accessible pagination controls
- screen-reader friendly loading and error states
- degraded indication must not rely on color only

## 10. Non-goals

This contract does not introduce:

- React router implementation
- click handler implementation
- `useNavigate`
- mutation APIs
- keyboard event implementation
