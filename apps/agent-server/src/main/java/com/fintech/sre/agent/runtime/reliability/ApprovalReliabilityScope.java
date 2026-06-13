package com.fintech.sre.agent.runtime.reliability;

public enum ApprovalReliabilityScope {
	RECOMMENDATION,
	HUMAN_APPROVAL,
	OPERATOR_CONTEXT,
	ROLLBACK_BOUNDARY,
	VERIFICATION_BOUNDARY,
	PAYMENT_SAFETY,
	LIFECYCLE,
	OPERATOR_VIEW
}
