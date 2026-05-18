package com.fintech.sre.agent.incident.lifecycle;

public record IncidentLifecycleTransitionResponse(
		String incidentLifecycleId,
		String incidentId,
		IncidentStatus previousStatus,
		IncidentStatus currentStatus,
		IncidentTransitionReason transitionReason,
		String operatorId,
		String summary
) {
}
