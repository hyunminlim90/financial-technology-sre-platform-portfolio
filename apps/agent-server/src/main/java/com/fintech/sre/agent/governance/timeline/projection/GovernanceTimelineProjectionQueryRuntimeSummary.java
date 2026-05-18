package com.fintech.sre.agent.governance.timeline.projection;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationMode;

public record GovernanceTimelineProjectionQueryRuntimeSummary(
		Instant checkedAt,
		GovernanceTimelineProjectionQueryRuntimeMode runtimeMode,
		GovernanceTimelineAggregationMode aggregationMode,
		boolean projectionBackedAvailable,
		boolean lightweight,
		List<String> signals,
		String summary
) {
}
