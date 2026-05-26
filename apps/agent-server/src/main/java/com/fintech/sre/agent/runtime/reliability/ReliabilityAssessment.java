package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ReliabilityAssessment(
		RuntimeState runtimeState,
		ReliabilityScore reliabilityScore,
		VerificationResult verificationResult,
		RollbackResult rollbackResult,
		PropagationSignal propagationSignal,
		OperationalUncertainty operationalUncertainty,
		EvidenceContext evidenceContext
) {
	public ReliabilityAssessment {
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(reliabilityScore, "reliabilityScore must not be null");
		Objects.requireNonNull(verificationResult, "verificationResult must not be null");
		Objects.requireNonNull(rollbackResult, "rollbackResult must not be null");
		Objects.requireNonNull(propagationSignal, "propagationSignal must not be null");
		Objects.requireNonNull(
				operationalUncertainty,
				"operationalUncertainty must not be null"
		);
		Objects.requireNonNull(evidenceContext, "evidenceContext must not be null");
	}

	public boolean requiresHumanReview() {
		return operationalUncertainty.requiresHumanEscalation()
				|| verificationResult == VerificationResult.INCONCLUSIVE
				|| verificationResult == VerificationResult.REGRESSION_DETECTED
				|| rollbackResult.failed()
				|| !evidenceContext.actionable();
	}

	public boolean degraded() {
		return runtimeState != RuntimeState.NORMAL || reliabilityScore.degraded();
	}
}
