package com.fintech.sre.agent.model.common;

public record MetricEvidence(
		String name,
		Double value,
		Double threshold,
		String status,
		String query
) {
}
