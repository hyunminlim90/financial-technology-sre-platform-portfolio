package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

public record GovernanceDashboardOverview(
		Instant generatedAt,
		GovernanceDashboardTimeRange timeRange,
		GovernanceDashboardDegradation degradation,
		GovernanceDashboardSummary summary,
		GovernanceDashboardBacklogSummary backlog,
		GovernanceDashboardTrendSummary trends,
		GovernanceDashboardRiskSummary riskIndicators
) {
}
