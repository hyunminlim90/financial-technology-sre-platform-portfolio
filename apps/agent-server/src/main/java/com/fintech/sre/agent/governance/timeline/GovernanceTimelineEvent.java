package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;

public record GovernanceTimelineEvent(
		String eventId,
		GovernanceTimelineEventType eventType,
		Instant occurredAt,
		String title,
		String summary,
		GovernanceTimelineSeverity severity,
		GovernanceTimelineActor actor,
		GovernanceTimelineResource resource,
		GovernanceTimelineEventMetadata metadata,
		boolean degraded
) {
}
