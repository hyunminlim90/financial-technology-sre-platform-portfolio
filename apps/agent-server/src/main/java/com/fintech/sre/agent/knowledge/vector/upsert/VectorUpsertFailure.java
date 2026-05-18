package com.fintech.sre.agent.knowledge.vector.upsert;

public record VectorUpsertFailure(
		String pointId,
		String reasonCode,
		String reason
) {
}
