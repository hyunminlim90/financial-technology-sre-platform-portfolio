package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.List;

public record GovernanceDashboardTrendSummary(
		Instant generatedAt,
		GovernanceDashboardTimeRange timeRange,
		GovernanceDashboardDegradation degradation,
		String bucketSize,
		List<GovernanceTrendSeries> series
) {
}
