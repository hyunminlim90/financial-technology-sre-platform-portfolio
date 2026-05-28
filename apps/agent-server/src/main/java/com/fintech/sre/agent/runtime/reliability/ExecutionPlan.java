package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionPlan(
		ExecutionPlanStatus status,
		ExecutionIntent intent,
		String rollbackPlanReference,
		String verificationPlanReference,
		ExecutionPlanRejectionReason rejectionReason
) {
	public ExecutionPlan {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(intent, "intent must not be null");
		if (status == ExecutionPlanStatus.REJECTED && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected execution plan requires rejection reason"
			);
		}
		if (status == ExecutionPlanStatus.STRUCTURED && rejectionReason != null) {
			throw new IllegalArgumentException(
					"structured execution plan must not contain rejection reason"
			);
		}
	}

	public boolean planOnly() {
		return true;
	}

	public boolean executes() {
		return false;
	}
}
