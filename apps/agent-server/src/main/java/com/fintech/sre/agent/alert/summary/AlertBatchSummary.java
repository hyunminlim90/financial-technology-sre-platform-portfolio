package com.fintech.sre.agent.alert.summary;

import java.util.Map;

public record AlertBatchSummary(
		int totalAlerts,
		int generatedRecommendations,
		int suppressedDuplicates,
		int rateLimitedAlerts,
		Map<String, Long> bySeverity,
		Map<String, Long> byService,
		Map<String, Long> byDomain,
		Map<String, Long> byStatus
) {
}
