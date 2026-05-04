package com.fintech.sre.agent.model.request;

import java.time.Instant;
import java.util.List;

public record RecommendationHistory(
		Instant recommendedAt,
		String failureMode,
		String confidenceLevel,
		List<String> recommendedActions,
		List<String> forbiddenActions,
		List<String> referencedKnowledge
) {
}
