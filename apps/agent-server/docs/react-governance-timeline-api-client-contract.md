# React Governance Timeline API Client Contract

## Purpose

This document defines the future React SRE Console client function contract for governance timeline APIs.

This phase adds documentation only.

It does not create a React project or implement real fetch functions.

## Client Function Signatures

```ts
export function fetchTimelinePage(
  params: TimelineQueryParams
): Promise<TimelineApiResponse>;

export function fetchTimelineHealth(): Promise<TimelineHealthResponse>;

export function fetchTimelineRuntimeSummary(): Promise<TimelineRuntimeSummaryResponse>;
```

## Query Params Type

```ts
export interface TimelineQueryParams {
  cursor?: string;
  direction?: "NEXT" | "PREVIOUS";
  limit?: number;
  from?: string;
  to?: string;
  eventType?: TimelineEventType[];
  includeDegraded?: boolean;
}
```

## Endpoint Mapping

- `fetchTimelinePage` -> `GET /internal/governance/timeline`
- `fetchTimelineHealth` -> `GET /internal/governance/timeline/health`
- `fetchTimelineRuntimeSummary` -> `GET /internal/governance/timeline/runtime-summary`

## Query Serialization Rules

- `cursor` is passed as an opaque string without parsing or rewriting.
- `direction` is serialized as `NEXT` or `PREVIOUS`.
- `limit` is serialized as a numeric query parameter.
- `from` and `to` are serialized as ISO-8601 strings.
- `eventType` is serialized as repeated query parameters.
- `includeDegraded` is serialized as a boolean query parameter when used.

Example:

```text
/internal/governance/timeline?eventType=RECOMMENDATION_CREATED&eventType=VERIFICATION_RECORDED
```

## Client Behavior Rules

- React project creation is out of scope.
- Real fetch implementation is out of scope.
- This document defines contract only.
- Backend DTO field names should be consumed without UI-side renaming assumptions.

## Degraded Response Handling

Degraded timeline responses must not be treated as thrown client errors by default.

Recommended behavior:

- return the `TimelineApiResponse` payload to the renderer
- allow the panel to render partial read-only data
- surface degraded metadata and degraded signals in UI state

## Error Handling Contract

### 400 `INVALID_TIMELINE_CURSOR`

Interpretation:

- cursor expired
- cursor invalid
- cursor tampered

Recommended UI handling:

- treat as a safe user-visible cursor state issue
- reset or refresh pagination state

### 400 `INVALID_TIMELINE_QUERY`

Interpretation:

- invalid direction
- invalid event type
- invalid from or to shape
- invalid bounded time range

Recommended UI handling:

- treat as a safe user-visible filter or query state issue

### 500 `TIMELINE_QUERY_FAILED`

Interpretation:

- backend timeline query failed unexpectedly

Recommended UI handling:

- treat as retry candidate
- or render a read-only error boundary state

## Forbidden Client Functions

The following functions must not exist in the timeline client contract:

- `approveRecommendation(...)`
- `executePlan(...)`
- `triggerRemediation(...)`
- `syncArgoCd(...)`
- `mutateGitOps(...)`
- `updateRag(...)`
- `ingestQdrant(...)`

## Read-only Contract

- timeline APIs are internal-only
- timeline clients are read-only
- timeline client functions must not trigger mutation workflows
