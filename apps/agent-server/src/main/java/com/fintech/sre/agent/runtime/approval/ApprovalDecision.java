package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ApprovalDecision(
		ApprovalDecisionLevel level,
		ApprovalDecisionReason reason,
		ApprovalDecisionScope scope,
		ApprovalStateIntegrationResult approvalStateIntegration,
		String decisionIdentifier,
		String approvalPolicy,
		String operatorContext,
		boolean decisionRationaleRequired,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ApprovalDecision {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				approvalStateIntegration,
				"approvalStateIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean humanApprovalPerformed() {
		return false;
	}

	public boolean approvalResult() {
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
