package com.fintech.sre.agent.knowledge.vector.upsert;

public record QdrantUpsertResponse(
		Object result,
		String status,
		double time
) {
	public boolean ok() {
		return "ok".equalsIgnoreCase(status());
	}
}
