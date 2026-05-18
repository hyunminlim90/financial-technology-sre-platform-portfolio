package com.fintech.sre.agent.learning.candidate;

import java.util.List;
import java.util.Map;

public record LearningCandidatePromotionRequest(
		LearningCandidateType type,
		String promotedBy,
		String summary,
		List<String> proposedChanges,
		Map<String, String> metadata
) {
}
