package com.fintech.sre.agent.observability.model;

public record TraceEvidence(
		String traceId,
		String spanName,
		String serviceName,
		Long durationMs,
		String status,
		String dependency,
		String query
) {
}
