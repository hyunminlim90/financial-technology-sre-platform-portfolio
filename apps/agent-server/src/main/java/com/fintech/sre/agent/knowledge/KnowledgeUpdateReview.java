package com.fintech.sre.agent.knowledge;

import java.time.Instant;
import java.util.List;

public record KnowledgeUpdateReview(
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
	public KnowledgeUpdateReview approve(String reason) {
		return new KnowledgeUpdateReview(
				id,
				incidentId,
				improvementCandidateId,
				type,
				KnowledgeUpdateStatus.APPROVED_BY_HUMAN,
				targetKnowledgePath,
				title,
				this.reason,
				evidence,
				proposedContentSummary,
				reason,
				createdAt,
				Instant.now()
		);
	}

	public KnowledgeUpdateReview reject(String reason) {
		return new KnowledgeUpdateReview(
				id,
				incidentId,
				improvementCandidateId,
				type,
				KnowledgeUpdateStatus.REJECTED_BY_HUMAN,
				targetKnowledgePath,
				title,
				this.reason,
				evidence,
				proposedContentSummary,
				reason,
				createdAt,
				Instant.now()
		);
	}

	public KnowledgeUpdateReview markAppliedExternally(String reason) {
		return new KnowledgeUpdateReview(
				id,
				incidentId,
				improvementCandidateId,
				type,
				KnowledgeUpdateStatus.APPLIED_EXTERNALLY,
				targetKnowledgePath,
				title,
				this.reason,
				evidence,
				proposedContentSummary,
				reason,
				createdAt,
				Instant.now()
		);
	}

	public KnowledgeUpdateReview cancel(String reason) {
		return new KnowledgeUpdateReview(
				id,
				incidentId,
				improvementCandidateId,
				type,
				KnowledgeUpdateStatus.CANCELLED,
				targetKnowledgePath,
				title,
				this.reason,
				evidence,
				proposedContentSummary,
				reason,
				createdAt,
				Instant.now()
		);
	}
}
