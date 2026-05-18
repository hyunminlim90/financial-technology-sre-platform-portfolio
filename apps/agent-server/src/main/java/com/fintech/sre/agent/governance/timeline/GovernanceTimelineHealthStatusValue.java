package com.fintech.sre.agent.governance.timeline;

public final class GovernanceTimelineHealthStatusValue {

	private GovernanceTimelineHealthStatusValue() {
	}

	public static double valueOf(GovernanceTimelineHealthStatus status) {
		if (status == null) {
			return 3.0;
		}

		return switch (status) {
			case HEALTHY -> 0.0;
			case DEGRADED_CAPABLE -> 1.0;
			case STRICT -> 2.0;
			case UNAVAILABLE -> 3.0;
		};
	}
}
