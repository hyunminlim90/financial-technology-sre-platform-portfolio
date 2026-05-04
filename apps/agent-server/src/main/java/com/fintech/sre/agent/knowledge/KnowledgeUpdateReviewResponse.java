package com.fintech.sre.agent.knowledge;

import java.time.Instant;
import java.util.List;

public record KnowledgeUpdateReviewResponse(
		String id,
		String incidentId,
		String improvementCandidateId,
		KnowledgeUpdateType type,
		KnowledgeUpdateStatus status,
		String targetKnowledgePath,
		String title,
		String reason,
		List<String> evidence,
		String proposedContentSummary,
		String humanDecisionReason,
		Instant createdAt,
		Instant updatedAt
) {
	public static KnowledgeUpdateReviewResponse from(KnowledgeUpdateReview review) {
		return new KnowledgeUpdateReviewResponse(
				review.id(),
				review.incidentId(),
				review.improvementCandidateId(),
				review.type(),
				review.status(),
				review.targetKnowledgePath(),
				review.title(),
				review.reason(),
				review.evidence(),
				review.proposedContentSummary(),
				review.humanDecisionReason(),
				review.createdAt(),
				review.updatedAt()
		);
	}
}
