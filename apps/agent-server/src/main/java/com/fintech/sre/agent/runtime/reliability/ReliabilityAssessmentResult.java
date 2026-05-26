package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ReliabilityAssessmentResult(
		RuntimeState runtimeState,
		List<ReliabilityAssessmentStage> stages,
		EvidenceCorrelation evidenceCorrelation,
		VerificationGateDecision verificationGateDecision,
		ConvergenceDecision convergenceDecision,
		RegressionDecision regressionDecision,
		OperationalUncertainty overallRisk,
		ReliabilityAssessmentRejectionReason rejectionReason
) {
	public ReliabilityAssessmentResult {
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(
				evidenceCorrelation,
				"evidenceCorrelation must not be null"
		);
		Objects.requireNonNull(overallRisk, "overallRisk must not be null");
		stages = List.copyOf(stages);
	}

	public boolean semanticOnly() {
		return true;
	}

	public boolean executionTrigger() {
		return false;
	}

	public boolean terminal() {
		return runtimeState == RuntimeState.FAILED;
	}
}
