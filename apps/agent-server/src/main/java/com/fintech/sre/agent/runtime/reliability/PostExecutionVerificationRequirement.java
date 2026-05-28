package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PostExecutionVerificationRequirement(
		ExecutorResponse executorResponse,
		ExecutionRequirement executionRequirement,
		EvidenceCorrelation evidenceCorrelation,
		boolean paymentConsistencyVerified,
		boolean rollbackTriggeredExecution,
		boolean rollbackVerified
) {
	public PostExecutionVerificationRequirement {
		Objects.requireNonNull(
				executorResponse,
				"executorResponse must not be null"
		);
		Objects.requireNonNull(
				executionRequirement,
				"executionRequirement must not be null"
		);
		Objects.requireNonNull(
				evidenceCorrelation,
				"evidenceCorrelation must not be null"
		);
	}

	public boolean paymentImpactingExecution() {
		return evidenceCorrelation.paymentSafetyEvidencePresent();
	}

	public boolean verificationIncomplete() {
		return evidenceCorrelation.completeness() != EvidenceCompleteness.COMPLETE
				|| !evidenceCorrelation.verificationEvidencePresent();
	}
}
