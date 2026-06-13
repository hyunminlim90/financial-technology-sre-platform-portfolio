package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceReliability(
		EvidenceReliabilityLevel level,
		EvidenceReliabilityReason reason,
		EvidenceReliabilityScope scope,
		EvidenceGovernancePolicy governancePolicy,
		EvidenceLineage lineage,
		EvidenceTrustScore trustScore,
		EvidenceConfidence confidence,
		boolean assessmentCertaintyAllowed,
		boolean paymentSafetyUncertainty
) {
	public EvidenceReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				governancePolicy,
				"governancePolicy must not be null"
		);
		Objects.requireNonNull(lineage, "lineage must not be null");
		Objects.requireNonNull(trustScore, "trustScore must not be null");
		Objects.requireNonNull(confidence, "confidence must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesEvidence() {
		return false;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmissionResult() {
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
