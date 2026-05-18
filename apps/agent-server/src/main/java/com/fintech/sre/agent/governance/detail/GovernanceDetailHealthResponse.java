package com.fintech.sre.agent.governance.detail;

import java.time.Instant;

public record GovernanceDetailHealthResponse(
		Instant checkedAt,
		GovernanceDetailHealthStatus status,
		boolean resilienceEnabled,
		boolean partialResponseEnabled,
		boolean failOpenDetail,
		int componentQueryTimeoutMs,
		String message
) {
}
