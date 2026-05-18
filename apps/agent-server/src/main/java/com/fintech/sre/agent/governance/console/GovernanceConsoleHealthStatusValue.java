package com.fintech.sre.agent.governance.console;

public final class GovernanceConsoleHealthStatusValue {

	private GovernanceConsoleHealthStatusValue() {
	}

	public static double valueOf(GovernanceConsoleHealthStatus status) {
		if (status == null) {
			return 2.0;
		}

		return switch (status) {
			case HEALTHY -> 0.0;
			case DEGRADED -> 1.0;
			case ATTENTION_REQUIRED -> 2.0;
		};
	}
}
