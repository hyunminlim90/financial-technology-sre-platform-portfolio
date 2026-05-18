package com.fintech.sre.agent.recommendation.execution;

import java.util.List;

public record ExecutionPlanResponse(
		String executionPlanId,
		String recommendationRecordId,
		String incidentId,
		ExecutionPlanStatus status,
		boolean executable,
		boolean requiresFinalApproval,
		List<ExecutionPlanStep> steps,
		List<String> blockedReasons
) {
	public ExecutionPlanResponse {
		steps = steps == null ? List.of() : List.copyOf(steps);
		blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
	}
}
