package com.fintech.sre.agent.learning.plan;

import java.util.List;

public record KnowledgePromotionPlanResponse(
		String promotionPlanId,
		String learningCandidateId,
		String incidentId,
		KnowledgePromotionPlanStatus status,
		String summary,
		List<KnowledgePromotionPlanTarget> targets,
		List<String> requiredHumanChecks,
		List<String> blockedReasons
) {
	public KnowledgePromotionPlanResponse {
		targets = targets == null ? List.of() : List.copyOf(targets);
		requiredHumanChecks = requiredHumanChecks == null ? List.of() : List.copyOf(requiredHumanChecks);
		blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
	}
}
