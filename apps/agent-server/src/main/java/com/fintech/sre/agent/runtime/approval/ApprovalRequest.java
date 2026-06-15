package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ApprovalRequest(
		ApprovalRequestLevel level,
		ApprovalRequestReason reason,
		ApprovalRequestScope scope,
		RecommendationPresentationIntegrationResult presentationIntegration,
		String operatorContext,
		boolean humanApprovalRequired,
		String approvalPolicy,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ApprovalRequest {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				presentationIntegration,
				"presentationIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean approval() {
		return false;
	}

	public boolean humanApproval() {
		return false;
	}

	public boolean approvalWorkflow() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}
}
