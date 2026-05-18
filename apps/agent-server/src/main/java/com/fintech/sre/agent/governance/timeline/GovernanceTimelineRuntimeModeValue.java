package com.fintech.sre.agent.governance.timeline;

public final class GovernanceTimelineRuntimeModeValue {

	private GovernanceTimelineRuntimeModeValue() {
	}

	public static double valueOf(GovernanceTimelineRuntimeMode mode) {
		if (mode == null) {
			return 2.0;
		}

		return switch (mode) {
			case NORMAL -> 0.0;
			case DEGRADED_READ_ONLY -> 1.0;
			case ATTENTION_REQUIRED -> 2.0;
		};
	}
}
