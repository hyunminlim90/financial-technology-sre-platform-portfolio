package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record SafetyPolicyDecision(
		boolean admitted,
		SafetyPolicyScope scope,
		SafetyPolicyRequirement requirement,
		SafetyPolicyRejectionReason rejectionReason
) {
	public SafetyPolicyDecision {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		if (!admitted && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected safety policy decision requires rejection reason"
			);
		}
		if (admitted && rejectionReason != null) {
			throw new IllegalArgumentException(
					"admitted safety policy decision must not contain rejection reason"
			);
		}
	}

	public boolean semanticSafetyAdmissionOnly() {
		return true;
	}

	public boolean executionPermission() {
		return false;
	}
}
