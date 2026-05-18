package com.fintech.sre.agent.governance.timeline;

public record GovernanceTimelineActor(
		GovernanceTimelineActorType type,
		String id,
		String displayName
) {
}
