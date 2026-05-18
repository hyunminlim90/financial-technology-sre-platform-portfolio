package com.fintech.sre.agent.recommendation.approval;

public enum RecommendationApprovalDecision {
	APPROVED,
	REJECTED;

	public RecommendationApprovalStatus toStatus() {
		return switch (this) {
			case APPROVED -> RecommendationApprovalStatus.APPROVED;
			case REJECTED -> RecommendationApprovalStatus.REJECTED;
		};
	}
}
