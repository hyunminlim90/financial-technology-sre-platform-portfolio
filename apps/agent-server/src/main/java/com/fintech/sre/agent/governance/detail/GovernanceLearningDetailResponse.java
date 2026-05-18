package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.List;

public record GovernanceLearningDetailResponse(
		Instant generatedAt,
		GovernanceDetailType type,
		String learningCandidateId,
		String incidentId,
		GovernanceDetailDegradation degradation,
		GovernanceDetailSummary learningCandidate,
		List<GovernanceDetailSummary> promotionReviews,
		List<GovernanceDetailSummary> promotionPlans,
		List<GovernanceDetailSummary> knowledgeUpdates,
		List<GovernanceDetailTimelineItem> timeline
) {
}
