package com.fintech.sre.agent.knowledge.embedding.local;

import java.util.List;

public record LocalEmbeddingRequest(
		String model,
		List<String> input
) {
}
