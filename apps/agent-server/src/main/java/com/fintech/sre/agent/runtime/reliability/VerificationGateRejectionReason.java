package com.fintech.sre.agent.runtime.reliability;

public enum VerificationGateRejectionReason {
	MISSING_VERIFICATION_EVIDENCE,
	PAYMENT_SAFETY_UNCERTAINTY,
	CONTRADICTORY_EVIDENCE,
	INSUFFICIENT_EVIDENCE_COMPLETENESS
}
