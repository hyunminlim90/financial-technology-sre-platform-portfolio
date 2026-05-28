package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PostExecutionConvergenceDecision(
		PostExecutionConvergenceStatus status,
		PostExecutionConvergenceRequirement requirement,
		PostExecutionConvergenceRejectionReason rejectionReason
) {
	public PostExecutionConvergenceDecision {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (status == PostExecutionConvergenceStatus.REJECTED
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected post execution convergence decision requires rejection reason"
			);
		}
		if (status == PostExecutionConvergenceStatus.CONVERGED
				&& rejectionReason != null) {
			throw new IllegalArgumentException(
					"converged post execution decision must not contain rejection reason"
			);
		}
		if (status == PostExecutionConvergenceStatus.INCOMPLETE
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"incomplete post execution convergence decision requires rejection reason"
			);
		}
	}

	public boolean converged() {
		return status == PostExecutionConvergenceStatus.CONVERGED;
	}

	public boolean executionResultOnly() {
		return false;
	}

	public boolean temporalSemanticStabilityOnly() {
		return true;
	}
}
