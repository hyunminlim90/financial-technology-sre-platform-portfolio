# React Governance Timeline State Contract

## Purpose

This document defines the future UI state contract for the React Governance Timeline panel.

This phase adds documentation only.

It does not create a React project, React hooks, Zustand or Redux stores, TanStack Query integration, or component implementation.

## State Types

```ts
export interface TimelinePanelState {
  runtime: TimelineRuntimeSummaryResponse | null;
  timeline: TimelineApiResponse | null;

  loading: boolean;
  loadingOlder: boolean;
  loadingNewer: boolean;

  error: TimelineErrorState | null;

  hasNext: boolean;
  hasPrevious: boolean;

  degraded: boolean;
  degradedSignals: string[];

  activeEventTypes: TimelineEventType[];
}

export interface TimelineLoadingState {
  initialLoading: boolean;
  loadingOlder: boolean;
  loadingNewer: boolean;
  refreshingRuntime: boolean;
}

export interface TimelineErrorState {
  code:
    | "INVALID_TIMELINE_CURSOR"
    | "INVALID_TIMELINE_QUERY"
    | "TIMELINE_QUERY_FAILED";

  message: string;
  retryable: boolean;
}

export interface TimelinePaginationState {
  nextCursor: string | null;
  previousCursor: string | null;

  hasNext: boolean;
  hasPrevious: boolean;
}
```

## State Transition Rules

The timeline panel should support the following state transitions:

- initial loading
- runtime loaded
- timeline loaded
- partial degraded timeline
- invalid cursor recovery
- retry after 5xx
- cursor pagination transition
- empty timeline state
- runtime `ATTENTION_REQUIRED` banner state

## Initial Loading

Initial state may be:

- `runtime = null`
- `timeline = null`
- `loading = true`
- `error = null`

Recommended transition:

1. request runtime summary
2. request initial timeline page
3. transition to loaded or error state

## Runtime Loaded

When `/internal/governance/timeline/runtime-summary` succeeds:

- `runtime` becomes non-null
- runtime badge and degraded signals become renderable
- `refreshingRuntime` may be false after first load

## Timeline Loaded

When `/internal/governance/timeline` succeeds:

- `timeline` becomes non-null
- `loading` becomes false
- `hasNext` and `hasPrevious` reflect current page metadata
- pagination cursors become renderable state only

## Partial Degraded Timeline

Degraded timeline is not a fatal UI error.

Meaning:

- partial degraded timeline is renderable
- `degraded = true`
- `degradedSignals` are informational only
- successful events remain visible
- `error` does not need to be set for degraded renderable state

## Invalid Cursor Recovery

When backend returns `INVALID_TIMELINE_CURSOR`:

- treat it as recoverable
- allow cursor reset and page reload
- do not treat it as fatal application failure

Recommended transition:

1. clear stale cursor state
2. preserve user-visible filters when possible
3. reload first valid page

## Retry Semantics

### `TIMELINE_QUERY_FAILED`

- retry candidate
- may be shown in read-only error state
- user-triggered retry is allowed

### `INVALID_TIMELINE_QUERY`

- not a retry candidate by default
- should be handled as invalid query or filter state

## Cursor Pagination Transition

Allowed transitions:

- `loadingOlder = true` while requesting `direction=NEXT`
- `loadingNewer = true` while requesting `direction=PREVIOUS`
- preserve existing rendered events while adjacent page request is in flight
- apply backend ordering without client-side re-sorting assumptions

## Empty Timeline State

When timeline request succeeds with empty items:

- `timeline` may still be non-null
- `error` remains null
- show neutral empty state

## Runtime ATTENTION_REQUIRED Banner State

When `runtime.runtimeMode === "ATTENTION_REQUIRED"`:

- render operator-facing attention banner state
- keep panel read-only
- do not infer mutation workflows from runtime state

## Forbidden State

The timeline panel state model must not include:

- approve state
- execute state
- remediation state
- GitOps mutation state
- Kubernetes mutation state
- RAG mutation state
- Qdrant mutation state
