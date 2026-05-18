package com.fintech.sre.agent.learning.plan;

import java.util.Map;

public record KnowledgePromotionPlanRequest(
		String plannedBy,
		String summary,
		Map<String, String> metadata
) {
}
