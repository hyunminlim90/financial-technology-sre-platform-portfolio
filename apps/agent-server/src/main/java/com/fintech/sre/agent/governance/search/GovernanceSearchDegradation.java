package com.fintech.sre.agent.governance.search;

import java.util.List;

public record GovernanceSearchDegradation(
		boolean degraded,
		boolean partialResult,
		List<String> failedComponents,
		String reason
) {
	public static GovernanceSearchDegradation none() {
		return new GovernanceSearchDegradation(false, false, List.of(), "none");
	}

	public static GovernanceSearchDegradation partial(
			List<String> failedComponents,
			String reason
	) {
		return new GovernanceSearchDegradation(
				true,
				true,
				failedComponents == null ? List.of() : List.copyOf(failedComponents),
				reason == null || reason.isBlank() ? "component_query_failed" : reason
		);
	}
}
