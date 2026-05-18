package com.fintech.sre.agent.incident.lifecycle;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class IncidentLifecycleTransitionValidator {

	public void validate(
			IncidentStatus from,
			IncidentStatus to
	) {
		if (to == null) {
			throw new IncidentLifecycleRejectedException(
					"INCIDENT_STATUS_REQUIRED",
					"toStatus is required."
			);
		}

		if (from == null) {
			return;
		}

		if (!allowed(from).contains(to)) {
			throw new IncidentLifecycleRejectedException(
					"INVALID_INCIDENT_TRANSITION",
					"Transition from " + from + " to " + to + " is not allowed."
			);
		}
	}

	private Set<IncidentStatus> allowed(
			IncidentStatus status
	) {
		return switch (status) {
			case OPEN -> Set.of(
					IncidentStatus.MITIGATING,
					IncidentStatus.ESCALATED
			);
			case MITIGATING -> Set.of(
					IncidentStatus.STABILIZING,
					IncidentStatus.ESCALATED,
					IncidentStatus.REOPENED
			);
			case STABILIZING -> Set.of(
					IncidentStatus.RESOLVED,
					IncidentStatus.REOPENED,
					IncidentStatus.ESCALATED
			);
			case RESOLVED -> Set.of(
					IncidentStatus.REOPENED
			);
			case REOPENED -> Set.of(
					IncidentStatus.MITIGATING,
					IncidentStatus.ESCALATED
			);
			case ESCALATED -> Set.of(
					IncidentStatus.MITIGATING,
					IncidentStatus.STABILIZING
			);
		};
	}
}
