package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

public record VerificationReadiness(
		VerificationReadinessLevel level,
		VerificationReadinessReason reason,
		VerificationReadinessScope scope,
		ApprovalReadiness approvalReadiness,
		boolean verificationEvidenceRequirementPresent
) {
	public VerificationReadiness {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				approvalReadiness,
				"approvalReadiness must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualVerificationExecution() {
		return false;
	}

	public boolean verificationRequestGeneration() {
		return false;
	}

	public boolean verificationWorkflow() {
		return false;
	}

	public boolean verificationReportGeneration() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}
}
