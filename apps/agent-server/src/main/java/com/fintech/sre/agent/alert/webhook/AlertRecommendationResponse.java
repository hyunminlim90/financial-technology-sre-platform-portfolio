package com.fintech.sre.agent.alert.webhook;

import java.util.List;

import com.fintech.sre.agent.alert.summary.AlertBatchSummary;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

public record AlertRecommendationResponse(
		String auditId,
		String status,
		int receivedAlerts,
		int generatedRecommendations,
		int suppressedDuplicates,
		List<String> suppressedAlertIds,
		int rateLimitedAlerts,
		List<String> rateLimitedAlertIds,
		AlertBatchSummary batchSummary,
		List<String> recommendationRecordIds,
		List<IncidentRecommendationResponse> recommendations
) {
}
