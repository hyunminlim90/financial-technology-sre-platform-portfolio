package com.fintech.sre.agent.knowledge.qdrant;

import java.util.List;
import java.util.Map;

public record QdrantSearchRequest(
		List<Float> vector,
		int limit,
		Map<String, Object> filter,
		boolean with_payload
) {
}
