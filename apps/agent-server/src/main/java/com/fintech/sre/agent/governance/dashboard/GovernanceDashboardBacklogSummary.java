package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.List;

public record GovernanceDashboardBacklogSummary(
		Instant generatedAt,
		GovernanceDashboardTimeRange timeRange,
		List<GovernanceBacklogItem> items,
		long pendingRecommendationApprovals,
		long approvedRecommendationsWithoutExecutionPlan,
		long executionResultsAwaitingVerification,
		long unresolvedIncidents,
		long postmortemDraftsAwaitingReview,
		long learningCandidatesAwaitingPromotionReview,
		long promotionPlansAwaitingApplication
) {
}
