package com.fintech.sre.agent.governance.detail;

import java.util.List;

public record GovernanceDetailDegradation(
		boolean degraded,
		boolean partialResponse,
		List<String> failedComponents,
		String reason
) {

	public static GovernanceDetailDegradation none() {
		return new GovernanceDetailDegradation(
				false,
				false,
				List.of(),
				"none"
		);
	}

	public static GovernanceDetailDegradation partial(
			List<String> failedComponents,
			String reason
	) {
		return new GovernanceDetailDegradation(
				true,
				true,
				failedComponents == null ? List.of() : failedComponents,
				reason == null || reason.isBlank()
						? "component_query_failed"
						: reason
		);
	}
}
