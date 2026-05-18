package com.fintech.sre.agent.recommendation.verification;

public record VerificationResultResponse(
		String verificationResultId,
		String executionResultId,
		String executionPlanId,
		String recommendationRecordId,
		String incidentId,
		VerificationStatus status,
		String operatorId,
		String summary
) {
}
