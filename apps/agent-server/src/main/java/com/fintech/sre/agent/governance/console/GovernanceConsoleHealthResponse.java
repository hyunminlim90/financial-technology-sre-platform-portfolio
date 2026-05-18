package com.fintech.sre.agent.governance.console;

import java.time.Instant;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthResponse;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthResponse;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthResponse;

public record GovernanceConsoleHealthResponse(
		Instant checkedAt,
		GovernanceConsoleHealthStatus overallStatus,
		GovernanceDashboardHealthResponse dashboardHealth,
		GovernanceDetailHealthResponse detailHealth,
		GovernanceSearchHealthResponse searchHealth,
		String message
) {
}
