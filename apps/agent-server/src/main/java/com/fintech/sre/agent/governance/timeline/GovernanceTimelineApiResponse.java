package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

public record GovernanceTimelineApiResponse(
		Instant respondedAt,
		GovernanceTimelineApiStatus status,
		GovernanceTimelinePageResponse page,
		GovernanceTimelineDegradation degradation,
		List<GovernanceTimelineApiError> errors
) {
}
