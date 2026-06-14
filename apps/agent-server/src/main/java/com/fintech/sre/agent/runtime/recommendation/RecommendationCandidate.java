package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadiness;

public record RecommendationCandidate(
		RecommendationCandidateLevel level,
		RecommendationCandidateReason reason,
		RecommendationCandidateScope scope,
		ActionAdmissionReadiness actionAdmissionReadiness,
		String runbookBinding
) {
	public RecommendationCandidate {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				actionAdmissionReadiness,
				"actionAdmissionReadiness must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean runbookSelection() {
		return false;
	}

	public boolean llmOutput() {
		return false;
	}

	public boolean approvalRequest() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}
}
