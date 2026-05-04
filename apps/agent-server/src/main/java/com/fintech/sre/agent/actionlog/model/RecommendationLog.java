package com.fintech.sre.agent.actionlog.model;

import java.time.Instant;
import java.util.List;

public record RecommendationLog(
		String recommendationId,
		String alertName,
		String service,
		String environment,
		String failureMode,
		String severity,
		String impactScope,
		String confidenceLevel,
		Instant createdAt,
		List<String> recommendedActions,
		List<String> forbiddenActions,
		List<String> referencedKnowledge
) {
}
