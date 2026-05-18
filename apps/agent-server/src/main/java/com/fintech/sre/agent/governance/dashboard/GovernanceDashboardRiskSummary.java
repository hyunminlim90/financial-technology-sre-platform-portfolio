package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.List;

public record GovernanceDashboardRiskSummary(
		Instant generatedAt,
		GovernanceDashboardTimeRange timeRange,
		GovernanceRiskLevel overallRiskLevel,
		List<GovernanceRiskIndicator> indicators
) {
}
