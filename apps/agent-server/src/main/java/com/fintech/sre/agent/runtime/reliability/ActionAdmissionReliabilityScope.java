package com.fintech.sre.agent.runtime.reliability;

public enum ActionAdmissionReliabilityScope {
	VERIFICATION,
	ACTION_TYPE,
	BLAST_RADIUS,
	ROLLBACK_BOUNDARY,
	VERIFICATION_BOUNDARY,
	HUMAN_APPROVAL,
	PAYMENT_SAFETY,
	LIFECYCLE,
	OPERATOR_VIEW
}
