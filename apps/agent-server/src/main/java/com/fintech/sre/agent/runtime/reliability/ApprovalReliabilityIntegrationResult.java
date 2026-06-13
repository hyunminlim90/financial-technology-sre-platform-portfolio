package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ApprovalReliabilityIntegrationResult(
		ApprovalReliability approvalReliability,
		EvidenceRuntimeApiResponse apiResponse,
		ApprovalReliabilityIntegrationStatus status,
		ApprovalReliabilityIntegrationReason reason,
		ApprovalReliabilityIntegrationScope scope,
		boolean approvalRequestAllowed,
		boolean approvalCertaintyAllowed
) {
	public ApprovalReliabilityIntegrationResult {
		Objects.requireNonNull(
				approvalReliability,
				"approvalReliability must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesApproval() {
		return false;
	}

	public boolean actualApproval() {
		return false;
	}

	public boolean approvalRequest() {
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
