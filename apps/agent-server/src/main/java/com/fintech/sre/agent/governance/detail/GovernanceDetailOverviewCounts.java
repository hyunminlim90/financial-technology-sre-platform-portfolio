package com.fintech.sre.agent.governance.detail;

public record GovernanceDetailOverviewCounts(
		int recommendations,
		int approvals,
		int executionPlans,
		int verifications,
		int postmortems,
		int learningCandidates,
		int knowledgeUpdates
) {
}
