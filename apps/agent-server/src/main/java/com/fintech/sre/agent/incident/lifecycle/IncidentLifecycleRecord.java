package com.fintech.sre.agent.incident.lifecycle;

import java.time.Instant;
import java.util.Map;

public record IncidentLifecycleRecord(
		String incidentLifecycleId,
		String incidentId,
		IncidentStatus previousStatus,
		IncidentStatus currentStatus,
		IncidentTransitionReason transitionReason,
		String operatorId,
		String summary,
		Instant transitionedAt,
		Map<String, String> metadata
) {
	public IncidentLifecycleRecord {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
