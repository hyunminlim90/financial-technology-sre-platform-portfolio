package com.fintech.sre.agent.governance.detail;

import java.time.Instant;

public record GovernanceDetailSummary(
		GovernanceDetailType type,
		String referenceId,
		String incidentId,
		String status,
		String title,
		String summary,
		Instant occurredAt
) {
}
