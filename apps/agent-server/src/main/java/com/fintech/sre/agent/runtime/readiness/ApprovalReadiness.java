package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

public record ApprovalReadiness(
		ApprovalReadinessLevel level,
		ApprovalReadinessReason reason,
		ApprovalReadinessScope scope,
		RecommendationReadiness recommendationReadiness,
		String operatorContext
) {
	public ApprovalReadiness {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				recommendationReadiness,
				"recommendationReadiness must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean approvalGeneration() {
		return false;
	}

	public boolean approvalRequestGeneration() {
		return false;
	}

	public boolean approvalWorkflow() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}
}
