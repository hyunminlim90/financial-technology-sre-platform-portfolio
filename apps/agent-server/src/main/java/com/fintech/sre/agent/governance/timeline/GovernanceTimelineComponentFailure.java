package com.fintech.sre.agent.governance.timeline;

public record GovernanceTimelineComponentFailure(
		GovernanceTimelineAggregationSource source,
		String reason
) {
}
