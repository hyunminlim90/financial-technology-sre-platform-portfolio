package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ApprovalReliability(
		ApprovalReliabilityLevel level,
		ApprovalReliabilityReason reason,
		ApprovalReliabilityScope scope,
		RecommendationReliability recommendationReliability,
		String operatorContext
) {
	public ApprovalReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				recommendationReliability,
				"recommendationReliability must not be null"
		);
	}

	public boolean readOnly() {
		return true;
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

	public boolean operatorFacingApprovalReadiness() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
