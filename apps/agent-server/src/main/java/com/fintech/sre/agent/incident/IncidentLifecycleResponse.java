package com.fintech.sre.agent.incident;

import java.time.Instant;
import java.util.List;

public record IncidentLifecycleResponse(
		String incidentId,
		IncidentStatus status,
		List<String> history,
		Instant createdAt,
		Instant updatedAt
) {
	public static IncidentLifecycleResponse from(IncidentLifecycle lifecycle) {
		return new IncidentLifecycleResponse(
				lifecycle.incidentId(),
				lifecycle.status(),
				lifecycle.history(),
				lifecycle.createdAt(),
				lifecycle.updatedAt()
		);
	}
}
