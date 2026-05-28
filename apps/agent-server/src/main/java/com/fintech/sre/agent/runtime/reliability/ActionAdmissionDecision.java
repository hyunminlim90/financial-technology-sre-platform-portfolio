package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ActionAdmissionDecision(
		boolean admitted,
		ActionAdmissionScope scope,
		ActionAdmissionRequirement requirement,
		ActionAdmissionRejectionReason rejectionReason
) {
	public ActionAdmissionDecision {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (!admitted && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected action admission decision requires rejection reason"
			);
		}
		if (admitted && rejectionReason != null) {
			throw new IllegalArgumentException(
					"admitted action admission decision must not contain rejection reason"
			);
		}
	}

	public boolean candidateAdmissionOnly() {
		return true;
	}

	public boolean executionPermission() {
		return false;
	}
}
