package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ApprovalState(
		ApprovalStateLevel level,
		ApprovalStateReason reason,
		ApprovalStateScope scope,
		ApprovalRequestIntegrationResult approvalRequestIntegration,
		String approvalStateIdentifier,
		String approvalPolicy,
		String operatorContext,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ApprovalState {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				approvalRequestIntegration,
				"approvalRequestIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean humanApprovalCompleted() {
		return false;
	}

	public boolean approvalDecision() {
		return false;
	}

	public boolean verificationRequest() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}
}
