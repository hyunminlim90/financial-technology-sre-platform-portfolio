package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionBoundaryDecision(
		boolean executionEligible,
		ExecutionScope scope,
		ExecutionRequirement requirement,
		ExecutionBoundaryRejectionReason rejectionReason
) {
	public ExecutionBoundaryDecision {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (!executionEligible && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected execution boundary decision requires rejection reason"
			);
		}
		if (executionEligible && rejectionReason != null) {
			throw new IllegalArgumentException(
					"eligible execution boundary decision must not contain rejection reason"
			);
		}
	}

	public boolean boundaryOnly() {
		return true;
	}

	public boolean executes() {
		return false;
	}
}
