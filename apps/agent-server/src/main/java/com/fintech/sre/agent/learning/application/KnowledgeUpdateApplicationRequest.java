package com.fintech.sre.agent.learning.application;

import java.util.List;
import java.util.Map;

public record KnowledgeUpdateApplicationRequest(
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
		Map<String, String> metadata
) {
}
