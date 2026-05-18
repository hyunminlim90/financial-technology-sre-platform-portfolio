package com.fintech.sre.agent.recommendation.execution.result;

public record HumanExecutionResultResponse(
		String executionResultId,
		String executionPlanId,
		String recommendationRecordId,
		String incidentId,
		HumanExecutionStatus status,
		String operatorId,
		String summary
) {
}
