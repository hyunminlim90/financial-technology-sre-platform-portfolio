package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record VerificationReliabilityIntegrationResult(
		VerificationReliability verificationReliability,
		EvidenceRuntimeApiResponse apiResponse,
		VerificationReliabilityIntegrationStatus status,
		VerificationReliabilityIntegrationReason reason,
		VerificationReliabilityIntegrationScope scope,
		boolean verificationRequestAllowed,
		boolean verificationCertaintyAllowed
) {
	public VerificationReliabilityIntegrationResult {
		Objects.requireNonNull(
				verificationReliability,
				"verificationReliability must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesVerification() {
		return false;
	}

	public boolean actualVerification() {
		return false;
	}

	public boolean verificationRequest() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
