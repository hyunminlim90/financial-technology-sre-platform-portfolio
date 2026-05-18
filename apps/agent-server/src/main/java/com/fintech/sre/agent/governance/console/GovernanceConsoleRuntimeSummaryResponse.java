package com.fintech.sre.agent.governance.console;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthResponse;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthResponse;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeSummaryResponse;

public record GovernanceConsoleRuntimeSummaryResponse(
		Instant checkedAt,
		GovernanceConsoleRuntimeMode runtimeMode,
		GovernanceConsoleHealthResponse consoleHealth,
		GovernanceDashboardHealthResponse dashboardHealth,
		GovernanceDetailHealthResponse detailHealth,
		GovernanceSearchHealthResponse searchHealth,
		GovernanceTimelineRuntimeSummaryResponse timelineRuntime,
		List<String> degradedSignals,
		String message
) {
}
