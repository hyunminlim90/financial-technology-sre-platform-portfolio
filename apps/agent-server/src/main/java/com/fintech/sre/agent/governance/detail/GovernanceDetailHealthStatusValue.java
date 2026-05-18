package com.fintech.sre.agent.governance.detail;

public final class GovernanceDetailHealthStatusValue {

	private GovernanceDetailHealthStatusValue() {
	}

	public static double valueOf(
			GovernanceDetailHealthStatus status
	) {
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
