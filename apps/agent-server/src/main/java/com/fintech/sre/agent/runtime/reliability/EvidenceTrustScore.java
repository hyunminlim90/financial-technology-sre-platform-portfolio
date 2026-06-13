package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceTrustScore(
		EvidenceTrustScoreLevel level,
		EvidenceTrustScoreReason reason,
		EvidenceTrustScoreScope scope,
		EvidenceGovernanceIntegrationResult governanceIntegrationResult,
		EvidenceLineageIntegrationResult lineageIntegrationResult
) {
	public EvidenceTrustScore {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				governanceIntegrationResult,
				"governanceIntegrationResult must not be null"
		);
		Objects.requireNonNull(
				lineageIntegrationResult,
				"lineageIntegrationResult must not be null"
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
