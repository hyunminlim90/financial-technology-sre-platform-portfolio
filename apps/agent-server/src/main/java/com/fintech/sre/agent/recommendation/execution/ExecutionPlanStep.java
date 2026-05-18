package com.fintech.sre.agent.recommendation.execution;

import java.util.Map;

public record ExecutionPlanStep(
		String actionType,
		String targetService,
		String targetLayer,
		String riskLevel,
		String blastRadius,
		boolean dryRunOnly,
		boolean rollbackRequired,
		boolean rollbackAvailable,
		boolean verificationRequired,
		boolean verificationAvailable,
		Map<String, String> parameters
) {
	public ExecutionPlanStep {
		parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
	}
}
