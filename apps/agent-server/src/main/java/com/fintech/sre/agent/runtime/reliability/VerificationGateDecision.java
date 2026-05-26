package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record VerificationGateDecision(
		boolean admitted,
		VerificationRequirement requirement,
		VerificationGateRejectionReason rejectionReason
) {
	public VerificationGateDecision {
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (!admitted && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected verification gate decision requires rejection reason"
			);
		}
		if (admitted && rejectionReason != null) {
			throw new IllegalArgumentException(
					"admitted verification gate decision must not contain rejection reason"
			);
		}
	}

	public static VerificationGateDecision admitted(
			VerificationRequirement requirement
	) {
		return new VerificationGateDecision(true, requirement, null);
	}

	public static VerificationGateDecision rejected(
			VerificationRequirement requirement,
			VerificationGateRejectionReason rejectionReason
	) {
		return new VerificationGateDecision(false, requirement, rejectionReason);
	}

	public boolean executionTrigger() {
		return false;
	}
}
