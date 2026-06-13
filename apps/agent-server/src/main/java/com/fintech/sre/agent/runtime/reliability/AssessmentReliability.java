package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record AssessmentReliability(
		AssessmentReliabilityLevel level,
		AssessmentReliabilityReason reason,
		AssessmentReliabilityScope scope,
		EvidenceReliability evidenceReliability,
		boolean assessmentCertaintyAllowed
) {
	public AssessmentReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				evidenceReliability,
				"evidenceReliability must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean exposesVendorDetail() {
		return false;
	}

	public boolean exposesCredentialConfiguration() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
