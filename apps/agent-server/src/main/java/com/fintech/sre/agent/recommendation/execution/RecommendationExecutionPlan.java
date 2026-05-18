package com.fintech.sre.agent.recommendation.execution;

import java.time.Instant;
import java.util.List;

public record RecommendationExecutionPlan(
		String executionPlanId,
		String recommendationRecordId,
		String incidentId,
		ExecutionPlanStatus status,
		boolean executable,
		boolean requiresFinalApproval,
		String createdBy,
		String reason,
		Instant createdAt,
		List<ExecutionPlanStep> steps,
		List<String> blockedReasons
) {
	public RecommendationExecutionPlan {
		steps = steps == null ? List.of() : List.copyOf(steps);
		blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
	}
}
