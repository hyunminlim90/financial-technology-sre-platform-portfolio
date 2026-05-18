package com.fintech.sre.agent.governance.timeline;

public record GovernanceTimelineResource(
		GovernanceTimelineResourceType type,
		String id,
		String displayName
) {
}
