package com.fintech.sre.agent.learning.promotion;

import java.time.Instant;
import java.util.Map;

public record KnowledgePromotionReviewRecord(
		String promotionReviewId,
		String learningCandidateId,
		String incidentId,
		KnowledgePromotionReviewStatus status,
		String reviewedBy,
		String reviewReason,
		String reviewSummary,
		Instant reviewedAt,
		Map<String, String> metadata
) {
	public KnowledgePromotionReviewRecord {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
