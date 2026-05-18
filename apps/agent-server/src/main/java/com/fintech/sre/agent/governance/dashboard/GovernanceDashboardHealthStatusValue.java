package com.fintech.sre.agent.governance.dashboard;

public final class GovernanceDashboardHealthStatusValue {

	private GovernanceDashboardHealthStatusValue() {
	}

	public static double valueOf(GovernanceDashboardHealthStatus status) {
		if (status == null) {
			return 2.0;
		}

		return switch (status) {
			case HEALTHY -> 0.0;
			case DEGRADED -> 1.0;
			case UNAVAILABLE -> 2.0;
		};
	}
}
