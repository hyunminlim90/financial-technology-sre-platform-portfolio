package com.fintech.sre.agent.runtime.verification;

import java.util.Objects;

public record VerificationRequestIntegrationResult(
		VerificationRequest verificationRequest,
		VerificationRequestIntegrationStatus status,
		VerificationRequestIntegrationReason reason,
		VerificationRequestIntegrationScope scope,
		boolean workflowEntryReady,
		boolean verificationRequestCertaintyAllowed
) {
	public VerificationRequestIntegrationResult {
		Objects.requireNonNull(
				verificationRequest,
				"verificationRequest must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean verificationRequestGeneration() {
		return false;
	}

	public boolean verificationWorkflow() {
		return false;
	}

	public boolean verificationResult() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
