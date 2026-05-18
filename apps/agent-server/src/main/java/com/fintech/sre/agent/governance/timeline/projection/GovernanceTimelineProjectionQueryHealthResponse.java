package com.fintech.sre.agent.governance.timeline.projection;

import java.time.Instant;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationMode;

public record GovernanceTimelineProjectionQueryHealthResponse(
		Instant checkedAt,
		GovernanceTimelineProjectionQueryHealthStatus status,
		GovernanceTimelineAggregationMode aggregationMode,
		boolean projectionBackedAvailable,
		boolean lightweight,
		String message
) {
}
