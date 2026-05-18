package com.fintech.sre.agent.incident.lifecycle;

import java.util.Map;

public record IncidentLifecycleTransitionRequest(
		IncidentStatus toStatus,
		IncidentTransitionReason transitionReason,
		String operatorId,
		String summary,
		Map<String, String> metadata
) {
}
