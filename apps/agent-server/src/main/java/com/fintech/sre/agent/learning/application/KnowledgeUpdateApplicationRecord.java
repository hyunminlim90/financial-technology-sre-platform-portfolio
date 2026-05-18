package com.fintech.sre.agent.learning.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record KnowledgeUpdateApplicationRecord(
		String knowledgeUpdateApplicationId,
		String incidentId,
		String learningCandidateId,
		String promotionPlanId,
		String knowledgeType,
		KnowledgeUpdateLayer knowledgeLayer,
		String filePath,
		KnowledgeUpdateChangeType changeType,
		String gitRepository,
		String gitBranch,
		String gitCommitSha,
		String pullRequestReference,
		String appliedBy,
		String reviewedBy,
		String approvedBy,
		List<String> validationChecks,
		Instant appliedAt,
		Map<String, String> metadata
) {
	public KnowledgeUpdateApplicationRecord {
		validationChecks = validationChecks == null ? List.of() : List.copyOf(validationChecks);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
