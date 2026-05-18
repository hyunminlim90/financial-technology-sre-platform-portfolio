package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.List;

public record GovernanceRecommendationDetailResponse(
		Instant generatedAt,
		GovernanceDetailType type,
		String recommendationRecordId,
		String incidentId,
		GovernanceDetailSummary summary,
		GovernanceDetailDegradation degradation,
		GovernanceDetailSummary recommendation,
		List<GovernanceDetailSummary> approvals,
		List<GovernanceDetailSummary> executionPlans,
		List<GovernanceDetailSummary> humanExecutionResults,
		List<GovernanceDetailSummary> verifications,
		List<GovernanceDetailTimelineItem> timeline
) {
}
