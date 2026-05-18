package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.List;

public record GovernanceKnowledgeUpdateDetailResponse(
		Instant generatedAt,
		GovernanceDetailType type,
		String knowledgeUpdateApplicationId,
		String incidentId,
		GovernanceDetailSummary summary,
		GovernanceDetailDegradation degradation,
		GovernanceDetailSummary knowledgeUpdate,
		GovernanceDetailSummary learningCandidate,
		GovernanceDetailSummary promotionPlan,
		List<GovernanceDetailSummary> promotionReviews,
		String knowledgeType,
		String knowledgeLayer,
		String filePath,
		String changeType,
		String gitRepository,
		String gitBranch,
		String gitCommitSha,
		String pullRequestReference,
		List<String> validationChecks,
		List<GovernanceDetailTimelineItem> timeline
) {
}
