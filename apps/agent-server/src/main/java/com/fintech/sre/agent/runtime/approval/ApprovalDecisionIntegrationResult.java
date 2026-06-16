package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

public record ApprovalDecisionIntegrationResult(
		ApprovalDecision approvalDecision,
		ApprovalDecisionIntegrationStatus status,
		ApprovalDecisionIntegrationReason reason,
		ApprovalDecisionIntegrationScope scope,
		boolean operatorFacingDecisionPendingVisible,
		boolean decisionPendingCertaintyAllowed
) {
	public ApprovalDecisionIntegrationResult {
		Objects.requireNonNull(
				approvalDecision,
				"approvalDecision must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean humanApproval() {
		return false;
	}

	public boolean approvalResult() {
		return false;
	}

	public boolean approvalWorkflow() {
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

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
