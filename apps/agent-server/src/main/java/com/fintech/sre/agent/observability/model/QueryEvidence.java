package com.fintech.sre.agent.observability.model;

public record QueryEvidence(
		String name,
		String query,
		Double threshold,
		String unit,
		String description
) {
}
