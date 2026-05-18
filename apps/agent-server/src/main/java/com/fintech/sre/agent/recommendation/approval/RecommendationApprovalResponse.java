package com.fintech.sre.agent.recommendation.approval;

public record RecommendationApprovalResponse(
		String approvalId,
		String recommendationRecordId,
		String incidentId,
		RecommendationApprovalStatus status,
		String operatorId,
		String reason
) {
}
