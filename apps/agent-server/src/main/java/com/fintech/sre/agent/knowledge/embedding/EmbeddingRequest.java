package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;
import java.util.Map;

public record EmbeddingRequest(
		String chunkId,
		String documentId,
		String input,
		Map<String, Object> payload,
		List<String> tags
) {
	public EmbeddingRequest {
		payload = payload == null ? Map.of() : Map.copyOf(payload);
		tags = tags == null ? List.of() : List.copyOf(tags);
	}
}
