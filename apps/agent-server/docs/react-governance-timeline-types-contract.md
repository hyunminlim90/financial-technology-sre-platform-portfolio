# React Governance Timeline Types Contract

## Purpose

This document defines the TypeScript type contract for consuming governance timeline backend APIs from the React SRE Console.

This phase adds documentation only.

It does not create a React project or generate TypeScript files.

Backend DTO names and field names should remain aligned with the documented contract.

## Status and Enum Types

```ts
export type TimelineApiStatus =
  | "SUCCESS"
  | "DEGRADED"
  | "FAILURE";

export type TimelineRuntimeMode =
  | "NORMAL"
  | "DEGRADED_READ_ONLY"
  | "ATTENTION_REQUIRED";

export type TimelineHealthStatus =
  | "HEALTHY"
  | "DEGRADED_CAPABLE"
  | "STRICT"
  | "UNAVAILABLE";

export type TimelineEventType =
  | "RECOMMENDATION_CREATED"
  | "APPROVAL_DECIDED"
  | "EXECUTION_PLAN_CREATED"
  | "HUMAN_EXECUTION_RECORDED"
  | "VERIFICATION_RECORDED"
  | "INCIDENT_TRANSITIONED"
  | "POSTMORTEM_DRAFT_CREATED"
  | "POSTMORTEM_REVIEWED"
  | "LEARNING_CANDIDATE_CREATED"
  | "PROMOTION_REVIEWED"
  | "PROMOTION_PLAN_CREATED"
  | "KNOWLEDGE_UPDATED";

export type TimelineSeverity =
  | "INFO"
  | "WARNING"
  | "ERROR"
  | "CRITICAL";
```

## Current HTTP API Payload Types

The current `GET /internal/governance/timeline` API returns a lightweight page item shape aligned with backend DTO field names.

```ts
export interface TimelineApiError {
  code: string;
  message: string;
}

export interface TimelineComponentFailure {
  source: string;
  reason: string;
}

export interface TimelineDegradation {
  degraded: boolean;
  partialTimeline: boolean;
  mode: string;
  failedComponents: TimelineComponentFailure[];
  reason: string;
}

export interface TimelinePageItem {
  occurredAt: string;
  type: string;
  recordId: string;
  status: string;
  title: string;
  summary: string;
}

export interface TimelinePageMetadata {
  nextCursor: string | null;
  previousCursor: string | null;
  hasNext: boolean;
  hasPrevious: boolean;
  limit: number;
  direction: "NEXT" | "PREVIOUS";
  ordering: string;
  degraded: boolean;
  failedComponents: string[];
}

export interface TimelinePageResponse {
  items: TimelinePageItem[];
  page: TimelinePageMetadata;
}

export interface TimelineApiResponse {
  respondedAt: string;
  status: TimelineApiStatus;
  page: TimelinePageResponse | null;
  degradation: TimelineDegradation | null;
  errors: TimelineApiError[];
}
```

## Normalized Timeline Event Contract

The backend also defines a richer normalized timeline event contract for future or domain-level integration.

```ts
export interface TimelineActor {
  type: "AI" | "HUMAN" | "SYSTEM";
  id: string;
  displayName: string;
}

export interface TimelineResource {
  type:
    | "INCIDENT"
    | "RECOMMENDATION"
    | "APPROVAL"
    | "EXECUTION_PLAN"
    | "HUMAN_EXECUTION"
    | "VERIFICATION"
    | "POSTMORTEM"
    | "LEARNING"
    | "KNOWLEDGE_PROMOTION"
    | "KNOWLEDGE_UPDATE";
  id: string;
  displayName: string;
}

export interface TimelineEventMetadata {
  attributes: Record<string, string>;
}

export interface TimelineEvent {
  eventId: string;
  eventType: TimelineEventType;
  occurredAt: string;
  title: string;
  summary: string;
  severity: TimelineSeverity;
  actor: TimelineActor;
  resource: TimelineResource;
  metadata: TimelineEventMetadata;
  degraded: boolean;
}
```

## Runtime and Health Response Types

```ts
export interface TimelineHealthResponse {
  checkedAt: string;
  status: TimelineHealthStatus;
  resilienceMode: string;
  partialTimelineSupported: boolean;
  failOpenReadOnly: boolean;
  streamingCompatible: boolean;
  degradedReasonTaxonomy: string[];
  message: string;
}

export interface TimelineRuntimeSummaryResponse {
  checkedAt: string;
  runtimeMode: TimelineRuntimeMode;
  healthStatus: TimelineHealthStatus;
  resilienceMode: string;
  partialTimelineSupported: boolean;
  failOpenReadOnly: boolean;
  streamingCompatible: boolean;
  degradedSignals: string[];
  message: string;
}
```

## Notes

- Cursor values are opaque strings.
- Cursor values must not be parsed by React clients.
- `eventId` should be used only for React key or display identity purposes.
- Mutation action types must not be introduced here.
- Approve, execute, and remediate action types are forbidden.
- Timeline API consumption remains internal-only and read-only.
