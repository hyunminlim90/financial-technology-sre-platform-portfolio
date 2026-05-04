package com.fintech.sre.agent.observability.model;

public record MetricEvidence(
		String name,
		Double value,
		Double threshold,
		String status,
		String query,
		String unit,
		String description
) {
}
