package com.fintech.sre.agent.recommendation.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RecommendationRecord(
		String recommendationRecordId,
		String incidentId,
		String auditId,
		String source,
		String service,
		String domain,
		String severity,
		String status,
		Instant generatedAt,
		int recommendedActionCount,
		int forbiddenActionCount,
		String policyDecision,
		String guardrailDecision,
		List<String> actionTypes,
		List<String> blockedReasons,
		Map<String, String> metadata
) {
	public RecommendationRecord {
		actionTypes = actionTypes == null ? List.of() : List.copyOf(actionTypes);
		blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
