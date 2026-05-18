package com.fintech.sre.agent.knowledge.embedding.local;

import java.util.List;

public record LocalEmbeddingResponse(
		List<Item> data
) {
	public record Item(
			int index,
			List<Float> embedding
	) {
	}
}
