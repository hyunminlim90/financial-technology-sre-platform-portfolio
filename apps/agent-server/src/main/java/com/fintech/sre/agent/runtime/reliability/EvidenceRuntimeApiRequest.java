package com.fintech.sre.agent.runtime.reliability;

public record EvidenceRuntimeApiRequest(
		EvidenceRuntimeSummaryResource summaryResource,
		EvidenceRuntimeSummary summary
) {
	public boolean authenticationAuthorizationDeferred() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
