package com.fintech.sre.agent.alert.audit;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.alert.summary.AlertBatchSummary;

public record AlertIngestionAuditLog(
		String auditId,
		Instant receivedAt,
		String source,
		String alertId,
		String alertName,
		String status,
		String severity,
		String service,
		String domain,
		String namespace,
		int generatedRecommendations,
		int suppressedDuplicates,
		List<String> suppressedAlertIds,
		int rateLimitedAlerts,
		List<String> rateLimitedAlertIds,
		AlertBatchSummary batchSummary,
		List<String> recommendationIds,
		List<String> errors
) {
}
