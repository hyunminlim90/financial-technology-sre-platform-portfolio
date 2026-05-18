package com.fintech.sre.agent.learning.application;

public record KnowledgeUpdateApplicationResponse(
		String knowledgeUpdateApplicationId,
		String incidentId,
		String learningCandidateId,
		String promotionPlanId,
		String filePath,
		String gitCommitSha,
		String appliedBy
) {
}
