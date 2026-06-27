package com.fintech.sre.agent.runtime.verification;

import java.util.Objects;

import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record VerificationRequest(
		VerificationRequestLevel level,
		VerificationRequestReason reason,
		VerificationRequestScope scope,
		ApprovalDecisionIntegrationResult approvalDecisionIntegration,
		String verificationRequestIdentifier,
		String verificationPolicy,
		boolean verificationEvidenceRequired,
		boolean rollbackBindingPresent,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public VerificationRequest {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				approvalDecisionIntegration,
				"approvalDecisionIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean verificationExecution() {
		return false;
	}

	public boolean verificationResult() {
		return false;
	}

	public boolean verificationWorkflow() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}
}
