package com.fintech.sre.agent.recommendation.execution;

public record ExecutionPlanRequest(
		String operatorId,
		String reason
) {
}
