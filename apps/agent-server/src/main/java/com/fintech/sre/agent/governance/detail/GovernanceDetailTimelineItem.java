package com.fintech.sre.agent.governance.detail;

import java.time.Instant;

public record GovernanceDetailTimelineItem(
		Instant occurredAt,
		String type,
		String recordId,
		String status,
		String title,
		String summary
) {
}
