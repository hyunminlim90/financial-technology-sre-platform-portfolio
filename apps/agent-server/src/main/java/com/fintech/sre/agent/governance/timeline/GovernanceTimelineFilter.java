package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

public record GovernanceTimelineFilter(
		String incidentId,
		String recommendationRecordId,
		String learningCandidateId,
		String knowledgeUpdateApplicationId,
		Instant from,
		Instant to,
		List<GovernanceTimelineEventType> eventTypes,
		boolean includeDegraded
) {
}
