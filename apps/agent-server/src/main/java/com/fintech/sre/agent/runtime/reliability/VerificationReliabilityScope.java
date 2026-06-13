package com.fintech.sre.agent.runtime.reliability;

public enum VerificationReliabilityScope {
	APPROVAL,
	VERIFICATION,
	VERIFICATION_EVIDENCE,
	ROLLBACK_BOUNDARY,
	PAYMENT_SAFETY,
	LIFECYCLE,
	OPERATOR_VIEW
}
