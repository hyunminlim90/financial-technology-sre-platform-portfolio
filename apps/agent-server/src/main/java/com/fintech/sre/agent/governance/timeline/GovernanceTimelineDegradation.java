package com.fintech.sre.agent.governance.timeline;

import java.util.List;

public record GovernanceTimelineDegradation(
		boolean degraded,
		boolean partialTimeline,
		GovernanceTimelineResilienceMode mode,
		List<GovernanceTimelineComponentFailure> failedComponents,
		String reason
) {
	public static GovernanceTimelineDegradation none() {
		return new GovernanceTimelineDegradation(
				false,
				false,
				GovernanceTimelineResilienceMode.STRICT,
				List.of(),
				"none"
		);
	}

	public static GovernanceTimelineDegradation partial(
			GovernanceTimelineResilienceMode mode,
			List<GovernanceTimelineComponentFailure> failedComponents,
			String reason
	) {
		return new GovernanceTimelineDegradation(
				true,
				true,
				mode == null
						? GovernanceTimelineResilienceMode.PARTIAL_DEGRADED
						: mode,
				failedComponents == null ? List.of() : List.copyOf(failedComponents),
				reason == null || reason.isBlank()
						? "aggregation_degraded"
						: reason
		);
	}
}
