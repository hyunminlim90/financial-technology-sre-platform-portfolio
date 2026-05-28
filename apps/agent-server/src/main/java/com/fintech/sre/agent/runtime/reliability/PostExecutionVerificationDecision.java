package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PostExecutionVerificationDecision(
		PostExecutionVerificationStatus status,
		PostExecutionVerificationRequirement requirement,
		PostExecutionVerificationRejectionReason rejectionReason
) {
	public PostExecutionVerificationDecision {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (status == PostExecutionVerificationStatus.REJECTED
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected post execution verification decision requires rejection reason"
			);
		}
		if (status == PostExecutionVerificationStatus.VERIFIED
				&& rejectionReason != null) {
			throw new IllegalArgumentException(
					"verified post execution verification decision must not contain rejection reason"
			);
		}
		if (status == PostExecutionVerificationStatus.INCOMPLETE
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"incomplete post execution verification decision requires rejection reason"
			);
		}
	}

	public boolean verified() {
		return status == PostExecutionVerificationStatus.VERIFIED;
	}

	public boolean converged() {
		return false;
	}

	public boolean executionInterpretationOnly() {
		return true;
	}
}
