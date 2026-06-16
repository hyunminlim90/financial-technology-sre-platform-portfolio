package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

public record ApprovalStateIntegrationResult(
		ApprovalState approvalState,
		ApprovalStateIntegrationStatus status,
		ApprovalStateIntegrationReason reason,
		ApprovalStateIntegrationScope scope,
		boolean operatorFacingPendingApprovalVisible,
		boolean pendingApprovalCertaintyAllowed
) {
	public ApprovalStateIntegrationResult {
		Objects.requireNonNull(approvalState, "approvalState must not be null");
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

	public boolean approvalDecision() {
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

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
