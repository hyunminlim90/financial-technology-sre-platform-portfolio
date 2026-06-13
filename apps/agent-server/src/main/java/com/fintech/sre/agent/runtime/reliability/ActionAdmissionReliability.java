package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ActionAdmissionReliability(
		ActionAdmissionReliabilityLevel level,
		ActionAdmissionReliabilityReason reason,
		ActionAdmissionReliabilityScope scope,
		VerificationReliability verificationReliability,
		String actionType,
		String blastRadiusBoundary
) {
	public ActionAdmissionReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				verificationReliability,
				"verificationReliability must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualActionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmissionResult() {
		return false;
	}

	public boolean operatorFacingAdmissionReadiness() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
