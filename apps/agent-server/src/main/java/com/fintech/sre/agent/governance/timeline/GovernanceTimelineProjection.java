package com.fintech.sre.agent.governance.timeline;

public record GovernanceTimelineProjection(
		GovernanceTimelineProjectionType sourceType,
		String sourceId,
		String incidentId,
		GovernanceTimelineEvent event
) {
}
