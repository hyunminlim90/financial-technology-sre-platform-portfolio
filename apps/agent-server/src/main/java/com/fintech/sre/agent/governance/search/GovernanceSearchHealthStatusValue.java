package com.fintech.sre.agent.governance.search;

public final class GovernanceSearchHealthStatusValue {

	private GovernanceSearchHealthStatusValue() {
	}

	public static double valueOf(GovernanceSearchHealthStatus status) {
		if (status == null) {
			return 2.0;
		}

		return switch (status) {
			case HEALTHY -> 0.0;
			case DEGRADED_CAPABLE -> 1.0;
			case STRICT -> 2.0;
		};
	}
}
