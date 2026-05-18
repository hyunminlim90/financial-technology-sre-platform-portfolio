package com.fintech.sre.agent.governance.timeline;

import java.util.List;

public record GovernanceTimelineAggregationRequest(
		GovernanceTimelineQuery query,
		List<GovernanceTimelineAggregationSource> sources
) {
	public List<GovernanceTimelineAggregationSource> safeSources() {
		if (sources == null || sources.isEmpty()) {
			return List.of(GovernanceTimelineAggregationSource.values());
		}

		return List.copyOf(sources);
	}
}
