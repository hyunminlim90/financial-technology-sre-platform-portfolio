package com.fintech.sre.agent.actionlog.entity;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

@Builder(toBuilder = true)
public record RecommendationActionEntity(
		Long id,
		String recommendationId,
		String incidentId,
		Integer step,
		String action,
		String expectedEffect,
		String risk,
		String rollbackPlan,
		List<String> verification,
		String source,
		String riskLevel,
		Boolean requiresHumanApproval,
		String status,
		Instant createdAt
) {
}
