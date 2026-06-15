package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

public record ApprovalRequestIntegrationResult(
		ApprovalRequest approvalRequest,
		ApprovalRequestIntegrationStatus status,
		ApprovalRequestIntegrationReason reason,
		ApprovalRequestIntegrationScope scope,
		boolean workflowEntryReady,
		boolean requestCertaintyAllowed
) {
	public ApprovalRequestIntegrationResult {
		Objects.requireNonNull(
				approvalRequest,
				"approvalRequest must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean approvalRequestGeneration() {
		return false;
	}

	public boolean approvalWorkflow() {
		return false;
	}

	public boolean humanApproval() {
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
