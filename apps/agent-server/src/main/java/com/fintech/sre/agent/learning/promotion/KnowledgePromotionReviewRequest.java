package com.fintech.sre.agent.learning.promotion;

import java.util.Map;

public record KnowledgePromotionReviewRequest(
		KnowledgePromotionReviewStatus status,
		String reviewedBy,
		String reviewReason,
		String reviewSummary,
		Map<String, String> metadata
) {
}
