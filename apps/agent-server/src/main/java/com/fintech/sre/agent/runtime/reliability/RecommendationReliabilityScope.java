package com.fintech.sre.agent.runtime.reliability;

public enum RecommendationReliabilityScope {
	DECISION,
	HUMAN_APPROVAL,
	ROLLBACK_BOUNDARY,
	VERIFICATION_BOUNDARY,
	PAYMENT_SAFETY,
	LIFECYCLE,
	OPERATOR_VIEW
}
