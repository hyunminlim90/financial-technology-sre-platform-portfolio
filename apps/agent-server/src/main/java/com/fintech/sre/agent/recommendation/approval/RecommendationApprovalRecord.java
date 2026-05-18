package com.fintech.sre.agent.recommendation.approval;

import java.time.Instant;
import java.util.Map;

public record RecommendationApprovalRecord(
		String approvalId,
		String recommendationRecordId,
		String incidentId,
		RecommendationApprovalStatus status,
		String operatorId,
		String reason,
		Instant decidedAt,
		Map<String, String> metadata
) {
	public RecommendationApprovalRecord {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
