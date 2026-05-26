package com.fintech.sre.agent.runtime.reliability;

public enum ReliabilityAssessmentRejectionReason {
	FAILED_STATE_TERMINAL,
	VERIFICATION_GATE_REJECTED,
	CONVERGENCE_REJECTED,
	REGRESSION_DETECTED,
	REGRESSION_PRIORITIZED_OVER_CONVERGENCE
}
