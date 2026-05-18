package com.fintech.sre.agent.governance.timeline;

import java.util.List;

public record GovernanceTimelineAggregationResult(
		GovernanceTimelinePageResponse page,
		boolean degraded,
		List<String> failedSources,
		String reason
) {
	public static GovernanceTimelineAggregationResult success(
			GovernanceTimelinePageResponse page
	) {
		return new GovernanceTimelineAggregationResult(
				page,
				false,
				List.of(),
				"none"
		);
	}

	public static GovernanceTimelineAggregationResult degraded(
			GovernanceTimelinePageResponse page,
			List<String> failedSources,
			String reason
	) {
		return new GovernanceTimelineAggregationResult(
				page,
				true,
				failedSources == null ? List.of() : List.copyOf(failedSources),
				reason == null || reason.isBlank()
						? "timeline_aggregation_degraded"
						: reason
		);
	}
}
