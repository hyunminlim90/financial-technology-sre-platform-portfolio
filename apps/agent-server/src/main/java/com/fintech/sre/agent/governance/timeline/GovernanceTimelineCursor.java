package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;

public record GovernanceTimelineCursor(
		Instant occurredAt,
		String eventType,
		String eventId
) {
}
