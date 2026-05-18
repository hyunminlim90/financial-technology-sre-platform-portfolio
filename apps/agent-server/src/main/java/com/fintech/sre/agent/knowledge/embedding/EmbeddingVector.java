package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;
import java.util.Map;

public record EmbeddingVector(
		String chunkId,
		String documentId,
		List<Float> vector,
		Map<String, Object> payload
) {
	public EmbeddingVector {
		vector = vector == null ? List.of() : List.copyOf(vector);
		payload = payload == null ? Map.of() : Map.copyOf(payload);
	}
}
