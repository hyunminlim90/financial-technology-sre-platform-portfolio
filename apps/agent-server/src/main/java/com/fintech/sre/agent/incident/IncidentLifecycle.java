package com.fintech.sre.agent.incident;

import java.time.Instant;
import java.util.List;

public record IncidentLifecycle(
		String incidentId,
		IncidentStatus status,
		List<String> history,
		Instant createdAt,
		Instant updatedAt
) {
	public IncidentLifecycle transitionTo(IncidentStatus next, String reason) {
		return new IncidentLifecycle(
				incidentId,
				next,
				appendHistory(next, reason),
				createdAt,
				Instant.now()
		);
	}

	private List<String> appendHistory(IncidentStatus next, String reason) {
		java.util.ArrayList<String> nextHistory = new java.util.ArrayList<>(
				history == null ? List.of() : history
		);

		nextHistory.add("%s -> %s : %s".formatted(
				Instant.now(),
				next,
				reason == null || reason.isBlank() ? "no reason" : reason
		));

		return List.copyOf(nextHistory);
	}
}
