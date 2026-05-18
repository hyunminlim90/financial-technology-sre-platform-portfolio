package com.fintech.sre.agent.governance.timeline;

import java.util.List;

public record GovernanceTimelineProjectionResult(
		List<GovernanceTimelineProjection> projections,
		boolean degraded,
		List<String> failedSources,
		String reason
) {
	public static GovernanceTimelineProjectionResult success(
			List<GovernanceTimelineProjection> projections
	) {
		return new GovernanceTimelineProjectionResult(
				projections == null ? List.of() : List.copyOf(projections),
				false,
				List.of(),
				"none"
		);
	}

	public static GovernanceTimelineProjectionResult degraded(
			List<GovernanceTimelineProjection> projections,
			List<String> failedSources,
			String reason
	) {
		return new GovernanceTimelineProjectionResult(
				projections == null ? List.of() : List.copyOf(projections),
				true,
				failedSources == null ? List.of() : List.copyOf(failedSources),
				reason == null || reason.isBlank()
						? "projection_failed"
						: reason
		);
	}
}
