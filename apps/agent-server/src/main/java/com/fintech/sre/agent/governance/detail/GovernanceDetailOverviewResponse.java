package com.fintech.sre.agent.governance.detail;

public record GovernanceDetailOverviewResponse(
		GovernanceDetailType type,
		String recordId,
		String title,
		String status,
		GovernanceDetailOverviewCounts counts,
		GovernanceDetailOverviewTimelineItem latestTimeline,
		GovernanceDetailDegradation degradation
) {
}
