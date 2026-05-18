package com.fintech.sre.agent.governance.search;

import java.time.Instant;

public record GovernanceSearchHealthResponse(
		Instant checkedAt,
		GovernanceSearchHealthStatus status,
		boolean resilienceEnabled,
		boolean partialSearchEnabled,
		boolean failOpenSearch,
		int componentQueryTimeoutMs,
		String message
) {
}
