package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.Map;

public record GovernanceDashboardSummary(
		Instant generatedAt,
		GovernanceDashboardTimeRange timeRange,
		GovernanceDashboardDegradation degradation,
		long recommendations,
		Map<String, Long> recommendationPolicyDecisions,
		GovernanceDashboardStatusBreakdown approvals,
		GovernanceDashboardStatusBreakdown executionPlans,
		GovernanceDashboardStatusBreakdown humanExecutions,
		GovernanceDashboardStatusBreakdown verifications,
		GovernanceDashboardStatusBreakdown incidents,
		GovernanceDashboardStatusBreakdown postmortemDrafts,
		GovernanceDashboardStatusBreakdown postmortemReviews,
		GovernanceDashboardStatusBreakdown learningCandidates,
		GovernanceDashboardStatusBreakdown promotionReviews,
		GovernanceDashboardStatusBreakdown promotionPlans,
		long knowledgeUpdates
) {
}
