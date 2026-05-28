package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionReadinessDecision(
		boolean ready,
		ExecutionReadinessScope scope,
		ExecutionReadinessRequirement requirement,
		ExecutionReadinessRejectionReason rejectionReason
) {
	public ExecutionReadinessDecision {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (!ready && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected execution readiness decision requires rejection reason"
			);
		}
		if (ready && rejectionReason != null) {
			throw new IllegalArgumentException(
					"ready execution readiness decision must not contain rejection reason"
			);
		}
	}

	public boolean semanticGateOnly() {
		return true;
	}

	public boolean executes() {
		return false;
	}
}
