package com.fintech.sre.agent.knowledge.vector.qdrant;

import java.util.List;
import java.util.Map;

public record QdrantSearchRequest(
		List<Double> vector,
		int limit,
		Double score_threshold,
		Map<String, Object> filter,
		boolean with_payload
) {
}
