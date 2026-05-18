package com.fintech.sre.agent.governance.dashboard;

public record GovernanceDashboardDegradation(
		boolean degraded,
		boolean fallbackUsed,
		String reason
) {
	public static GovernanceDashboardDegradation none() {
		return new GovernanceDashboardDegradation(false, false, "none");
	}

	public static GovernanceDashboardDegradation fallback(String reason) {
		return new GovernanceDashboardDegradation(true, true, reason);
	}
}
