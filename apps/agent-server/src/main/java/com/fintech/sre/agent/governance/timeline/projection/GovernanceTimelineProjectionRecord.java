package com.fintech.sre.agent.governance.timeline.projection;

import java.time.Instant;
import java.util.Map;

public record GovernanceTimelineProjectionRecord(
		String eventId,
		String eventType,
		Instant occurredAt,
		String sourceType,
		String sourceId,
		String incidentId,
		String recommendationRecordId,
		String learningCandidateId,
		String knowledgeUpdateApplicationId,
		String severity,
		String actorType,
		String resourceType,
		String title,
		String summary,
		Map<String, Object> metadata,
		boolean degraded,
		Instant createdAt
) {

	public GovernanceTimelineProjectionRecord {
		metadata = metadata == null
				? Map.of()
				: Map.copyOf(metadata);
	}
}
