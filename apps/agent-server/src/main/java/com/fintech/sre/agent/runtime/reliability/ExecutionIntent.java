package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionIntent(
		ExecutionBoundaryDecision executionBoundaryDecision,
		String rollbackPlanReference,
		String verificationPlanReference,
		boolean paymentImpacting,
		boolean paymentConsistencyVerificationIncluded,
		boolean aiOnlyApproved,
		boolean explicitExecutionAuthorized
) {
	public ExecutionIntent {
		Objects.requireNonNull(
				executionBoundaryDecision,
				"executionBoundaryDecision must not be null"
		);
	}
}
