package com.fintech.sre.agent.governance.detail;

import java.time.Instant;

public record GovernanceDetailOverviewTimelineItem(
		String type,
		String status,
		Instant occurredAt,
		String summary
) {
}
