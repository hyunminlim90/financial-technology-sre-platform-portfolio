package com.fintech.sre.agent.learning.candidate;

public record LearningCandidatePromotionResponse(
		String learningCandidateId,
		String incidentId,
		String postmortemDraftId,
		LearningCandidateType type,
		LearningCandidateStatus status,
		String summary
) {
}
