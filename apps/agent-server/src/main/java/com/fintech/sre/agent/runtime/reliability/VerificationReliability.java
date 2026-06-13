package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record VerificationReliability(
		VerificationReliabilityLevel level,
		VerificationReliabilityReason reason,
		VerificationReliabilityScope scope,
		ApprovalReliability approvalReliability,
		boolean verificationEvidenceRequirementPresent
) {
	public VerificationReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				approvalReliability,
				"approvalReliability must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualVerificationExecution() {
		return false;
	}

	public boolean verificationWorkflow() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}

	public boolean operatorFacingVerificationReadiness() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
