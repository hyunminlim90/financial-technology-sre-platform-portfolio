package com.fintech.sre.agent.governance.dashboard;

import java.util.Map;

public record GovernanceDashboardStatusBreakdown(
		long total,
		Map<String, Long> byStatus
) {
}
