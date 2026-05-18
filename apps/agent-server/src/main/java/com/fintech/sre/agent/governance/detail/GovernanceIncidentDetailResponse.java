package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.List;

public record GovernanceIncidentDetailResponse(
		Instant generatedAt,
		GovernanceDetailType type,
		String incidentId,
		GovernanceDetailSummary summary,
		String currentStatus,
		GovernanceDetailDegradation degradation,
		List<GovernanceDetailTimelineItem> timeline,
		List<GovernanceDetailSummary> recommendations,
		List<GovernanceDetailSummary> approvals,
		List<GovernanceDetailSummary> executionPlans,
		List<GovernanceDetailSummary> humanExecutionResults,
		List<GovernanceDetailSummary> verifications,
		List<GovernanceDetailSummary> postmortemDrafts,
		List<GovernanceDetailSummary> postmortemReviews,
		List<GovernanceDetailSummary> learningCandidates,
		List<GovernanceDetailSummary> knowledgeUpdates
) {
}
