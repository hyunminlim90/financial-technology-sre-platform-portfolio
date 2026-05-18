package com.fintech.sre.agent.recommendation.approval.audit;

import java.time.Instant;
import java.util.Map;

import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;

public record RecommendationApprovalAuditLog(
		String auditId,
		String recommendationRecordId,
		String incidentId,
		RecommendationApprovalStatus status,
		String operatorId,
		String reason,
		Instant decidedAt,
		Map<String, String> metadata
) {
}
