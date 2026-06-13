package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceGovernanceIntegrationResult(
		EvidenceGovernancePolicy governancePolicy,
		EvidenceRuntimeApiResponse apiResponse,
		EvidenceGovernanceIntegrationStatus status,
		EvidenceGovernanceIntegrationReason reason,
		EvidenceGovernanceIntegrationScope scope
) {
	public EvidenceGovernanceIntegrationResult {
		Objects.requireNonNull(
				governancePolicy,
				"governancePolicy must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
