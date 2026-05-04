package com.fintech.sre.agent.knowledge.vector.qdrant;

import java.util.List;
import java.util.Map;

public record QdrantSearchResponse(
		List<QdrantPoint> result
) {
	public record QdrantPoint(
			String id,
			Double score,
			Map<String, Object> payload
	) {
	}
}
