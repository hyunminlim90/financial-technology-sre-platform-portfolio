package com.fintech.sre.agent.model.common;

public record IncidentSummary(
		String failureMode,
		String domain,
		String service,
		String environment,
		Severity severity,
		ImpactScope impactScope
) {
}
