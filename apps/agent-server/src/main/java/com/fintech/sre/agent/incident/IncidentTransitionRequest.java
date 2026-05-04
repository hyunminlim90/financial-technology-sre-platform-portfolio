package com.fintech.sre.agent.incident;

public record IncidentTransitionRequest(
		IncidentStatus status,
		String reason
) {
}
