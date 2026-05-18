package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

public record GovernanceDashboardHealthResponse(
		Instant checkedAt,
		GovernanceDashboardHealthStatus status,
		boolean optimizedQueryAvailable,
		boolean fallbackEnabled,
		boolean failOpenDashboard,
		boolean resilienceEnabled,
		String lastDegradationReason,
		String message
) {
}
