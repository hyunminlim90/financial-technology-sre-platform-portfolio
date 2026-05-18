package com.fintech.sre.agent.knowledge.qdrant;

import java.util.Map;

public record QdrantScrollRequest(
		Map<String, Object> filter,
		int limit,
		boolean with_payload
) {
}
