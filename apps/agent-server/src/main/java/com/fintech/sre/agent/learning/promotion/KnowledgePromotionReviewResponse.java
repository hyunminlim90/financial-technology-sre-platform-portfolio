package com.fintech.sre.agent.learning.promotion;

public record KnowledgePromotionReviewResponse(
		String promotionReviewId,
		String learningCandidateId,
		String incidentId,
		KnowledgePromotionReviewStatus status,
		String reviewedBy,
		String reviewSummary
) {
}
