package com.fintech.sre.agent.runtime.reliability;

public enum DecisionReliabilityScope {
	ASSESSMENT,
	SCENARIO_BOUNDARY,
	ROLLBACK_BOUNDARY,
	VERIFICATION_BOUNDARY,
	PAYMENT_SAFETY,
	LIFECYCLE,
	OPERATOR_VIEW
}
