package com.fintech.sre.agent.learning.plan;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record KnowledgePromotionPlanRecord(
		String promotionPlanId,
		String learningCandidateId,
		String incidentId,
		KnowledgePromotionPlanStatus status,
		String plannedBy,
		String summary,
		List<KnowledgePromotionPlanTarget> targets,
		List<String> requiredHumanChecks,
		List<String> blockedReasons,
		Instant createdAt,
		Map<String, String> metadata
) {
	public KnowledgePromotionPlanRecord {
		targets = targets == null ? List.of() : List.copyOf(targets);
		requiredHumanChecks = requiredHumanChecks == null ? List.of() : List.copyOf(requiredHumanChecks);
		blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
